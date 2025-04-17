package com.tianji.learning.service;

import com.tianji.learning.pojo.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {
    /**
     * 查询历史赛季列表
     * @return
     */
    List<PointsBoardSeason> getHistorySeasonList();
}
