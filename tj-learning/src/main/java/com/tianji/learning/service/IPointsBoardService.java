package com.tianji.learning.service;

import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.pojo.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
public interface IPointsBoardService extends IService<PointsBoard> {
    /**
     * 分页查询指定赛季的积分排行榜
     * @param query
     * @return
     */
    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    /**
     * 创建上赛季排行榜
     * @param id
     */
    void createPointsBoardTableBySeason(Integer id);

    /**
     * 查询当前赛季积分排行榜
     * @param key
     * @param pageNo
     * @param pageSize
     * @return
     */
    List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize);

}
