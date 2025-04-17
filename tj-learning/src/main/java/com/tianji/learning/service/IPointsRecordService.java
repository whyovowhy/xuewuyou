package com.tianji.learning.service;

import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.pojo.PointsRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
public interface IPointsRecordService extends IService<PointsRecord> {
    /**
     * 获取积分
     * @param userId
     * @param points
     * @param sign
     */
    void addPointsRecord(Long userId, Integer points, PointsRecordType sign);

    /**
     * 查询我的今日积分
     * @return
     */
    List<PointsStatisticsVO> queryMyPointsToday();
}
