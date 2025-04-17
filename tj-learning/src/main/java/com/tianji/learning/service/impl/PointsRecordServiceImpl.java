package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.pojo.PointsRecord;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {
    private final StringRedisTemplate redisTemplate;
    /**
     * 获取积分
     *
     * @param userId
     * @param points
     * @param recordType
     */
    @Override
    public void addPointsRecord(Long userId, Integer points, PointsRecordType recordType) {
        int maxPoints = recordType.getMaxPoints();
        PointsRecord pointsRecord = new PointsRecord();
        pointsRecord.setUserId(userId);
        pointsRecord.setType(recordType);
        //判断是否存在积分限制
        LocalDateTime now = LocalDateTime.now();
        if(maxPoints>0){//存在
            //判断是否超出限制
            LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);
            LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);
            int DBPoints = queryUserPointsByTypeAndDate(userId, recordType, dayStartTime, dayEndTime);
            if(DBPoints+points>maxPoints){//超出限制
                return;
            }else {//未超出限制
                points=maxPoints-DBPoints;
            }
        }
        pointsRecord.setPoints(points);
        this.save(pointsRecord);
        //累加积分至redis积分表
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARDS_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key,userId.toString(),points);

    }

    /**
     * 查询我的今日积分
     *
     * @return
     */
    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1.获取用户id
        Long userId = UserContext.getUser();
        // 2.查询用户积分获取记录
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayEndTime = DateUtils.getDayEndTime(now);
        LocalDateTime dayStartTime = DateUtils.getDayStartTime(now);
        List<PointsRecord> pointsRecords = lambdaQuery()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, dayStartTime, dayEndTime)
                .list();
        if(CollUtils.isEmpty(pointsRecords)){
            return CollUtils.emptyList();
        }
        // 3.封装返回值
        List<PointsStatisticsVO> voList = new ArrayList<>(pointsRecords.size());
        for (PointsRecord p : pointsRecords) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            voList.add(vo);
        }
        return voList;
    }

    private int queryUserPointsByTypeAndDate(Long userId, PointsRecordType recordType, LocalDateTime dayStartTime, LocalDateTime dayEndTime) {
        // 1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(recordType != null, PointsRecord::getType, recordType)
                .between(dayStartTime != null && dayEndTime != null, PointsRecord::getCreateTime, dayStartTime, dayEndTime);
        // 2.调用mapper，查询结果
        Integer points = getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        // 3.判断并返回
        return points == null ? 0 : points;
    }
}
