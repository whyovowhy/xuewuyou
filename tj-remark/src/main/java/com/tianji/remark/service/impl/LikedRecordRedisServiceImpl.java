package com.tianji.remark.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domian.LikeRecordFormDTO;
import com.tianji.remark.domian.LikedTimesDTO;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.pojo.LikedRecord;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper rabbitMqHelper;
    private final StringRedisTemplate redisTemplate;
    /**
     * 点赞或取消点赞
     *
     * @param recordDTO
     */
    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        //1获取用户id
        Long userId = UserContext.getUser();
        //2判断点赞还是取消点赞
        boolean success=false;
        if (recordDTO.getLiked()) {//点赞
            success=like(userId, recordDTO);
        }else {//取消点赞
            success=unlike(userId, recordDTO);
        }
        if (!success) {
            return;
        }
        //3统计点赞总数
        String redisKey= RedisConstants.LIKE_BIZ_KEY_PREFIX+recordDTO.getBizId();
        Long allLikes = redisTemplate.opsForSet().size(redisKey);
        //4缓存到redis
        redisTemplate.opsForZSet().add(
                RedisConstants.LIKES_TIMES_KEY_PREFIX+recordDTO.getBizType(),
                recordDTO.getBizId().toString(),
                allLikes
        );
    }

    private boolean unlike(Long userId, LikeRecordFormDTO recordDTO) {
        String redisKey= RedisConstants.LIKE_BIZ_KEY_PREFIX+recordDTO.getBizId();
        Long remove = redisTemplate.opsForSet().remove(redisKey, userId.toString());
        return remove!=null&&remove>0;
    }

    private boolean like(Long userId, LikeRecordFormDTO recordDTO) {
        String redisKey= RedisConstants.LIKE_BIZ_KEY_PREFIX+recordDTO.getBizId();
        Long add = redisTemplate.opsForSet().add(redisKey, userId.toString());
        return add!=null&&add>0;
    }

    /**
     * 查询指定业务id的点赞状态
     *
     * @param bizIds
     * @return
     */
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        return null;
    }

    /**
     * 从redis取指定类型点赞数量并发送消息到RabbitMQ
     *
     * @param bizType    业务类型
     * @param maxBizSize 每次任务取出的业务score标准
     */
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        String redisKey= RedisConstants.LIKES_TIMES_KEY_PREFIX+bizType;
        //从redis取maxBizSize条指定类型点赞数量
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(redisKey, maxBizSize);

        if(CollUtils.isNotEmpty(typedTuples)) {
            List<LikedTimesDTO> likedTimesDTOS=new ArrayList<>(typedTuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
                LikedTimesDTO likedTimesDTO=new LikedTimesDTO();
                String bizId = tuple.getValue();
                Double bizLikes = tuple.getScore();
                // 校验是否为空
                if (StringUtils.isBlank(bizId) || bizLikes == null) {
                    continue;
                }
                likedTimesDTO.setLikedTimes(bizLikes.intValue());
                likedTimesDTO.setBizId(Long.valueOf(bizId));

                likedTimesDTOS.add(likedTimesDTO);
            }
            // 发送消息到RabbitMQ
            log.debug("发送点赞消息======>>>>>>{}",likedTimesDTOS);
            if(CollUtils.isNotEmpty(likedTimesDTOS)){
                String routingKey=StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, bizType);
                rabbitMqHelper.send(
                        MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                        routingKey,
                        likedTimesDTOS
                );
            }
        }
    }
}
