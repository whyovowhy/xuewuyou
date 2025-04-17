//package com.tianji.remark.service.impl;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.constants.MqConstants;
//import com.tianji.common.utils.BeanUtils;
//import com.tianji.common.utils.CollUtils;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.remark.domian.LikeRecordFormDTO;
//import com.tianji.remark.domian.LikedTimesDTO;
//import com.tianji.remark.pojo.LikedRecord;
//import com.tianji.remark.mapper.LikedRecordMapper;
//import com.tianji.remark.service.ILikedRecordService;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * <p>
// * 点赞记录表 服务实现类
// * </p>
// *
// * @author yizayu
// * @since 2024-10-14
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//    private final RabbitMqHelper rabbitMqHelper;
//
//    /**
//     * 点赞或取消点赞
//     *
//     * @param recordDTO
//     */
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
//        //1获取用户id
//        Long userId = UserContext.getUser();
//        //2判断点赞还是取消点赞
//        boolean success=false;
//        if (recordDTO.getLiked()) {//点赞
//            success=like(userId, recordDTO);
//        }else {//取消点赞
//            success=unlike(userId, recordDTO);
//        }
//        if (!success) {
//            return;
//        }
//        //3统计点赞数
//        Integer allLikes = lambdaQuery()
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .count();
//        String routingKey=StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE, recordDTO.getBizType());
//        LikedTimesDTO likedTimesDTO = new LikedTimesDTO(recordDTO.getBizId(), allLikes);
//        log.debug("发送点赞消息======>>>>>>{}",likedTimesDTO);
//        //4发送消息
//        rabbitMqHelper.send(
//                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
//                routingKey,
//                likedTimesDTO
//        );
//    }
//
//    /**
//     * 查询指定业务id的点赞状态
//     *
//     * @param bizIds
//     * @return
//     */
//    @Override
//    public Set<Long> isBizLiked(List<Long> bizIds) {
//        //1获取用户id
//        Long userId = UserContext.getUser();
//        //2查询点赞记录
//        List<LikedRecord> likedRecords = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .in(CollUtils.isNotEmpty(bizIds), LikedRecord::getBizId, bizIds)
//                .list();
//        return likedRecords.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//    }
//
//    /**
//     * 取消点赞
//     *
//     * @param userId
//     * @param recordDTO
//     * @return
//     */
//    private boolean unlike(Long userId, LikeRecordFormDTO recordDTO) {
//        LikedRecord DBlikedRecord = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .one();
//        if (DBlikedRecord == null) {
//            return false;
//        }
//        boolean remove = removeById(DBlikedRecord.getId());
//        return remove;
//    }
//
//    /**
//     * 点赞
//     *
//     * @param userId
//     * @param recordDTO
//     * @return
//     */
//    private boolean like(Long userId, LikeRecordFormDTO recordDTO) {
//        LikedRecord DBlikedRecord = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .one();
//        if (DBlikedRecord == null) {
//            LikedRecord likedRecord = BeanUtils.copyBean(recordDTO, LikedRecord.class);
//            likedRecord.setUserId(userId);
//            boolean save = save(likedRecord);
//            return save;
//        }
//        return false;
//    }
//}
