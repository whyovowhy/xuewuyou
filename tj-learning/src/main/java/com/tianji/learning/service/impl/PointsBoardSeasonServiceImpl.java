package com.tianji.learning.service.impl;

import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.pojo.PointsBoardSeason;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    /**
     * 查询历史赛季列表
     *
     * @return
     */
    @Override
    public List<PointsBoardSeason> getHistorySeasonList() {
        List<PointsBoardSeason> seasonList = this.lambdaQuery().list();
        if (CollUtils.isEmpty(seasonList)) {
            throw new BizIllegalException("查询历史赛季列表失败");
        }
        return seasonList;
    }
}
