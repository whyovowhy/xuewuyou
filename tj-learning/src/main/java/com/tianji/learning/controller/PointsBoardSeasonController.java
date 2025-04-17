package com.tianji.learning.controller;


import com.tianji.learning.pojo.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
@Api(tags = "PointsBoardSeason管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/boards/seasons")
public class PointsBoardSeasonController {

    private final IPointsBoardSeasonService pointsBoardSeasonService;

    /**
     * 查询历史赛季列表（无分页）
     */
    @ApiOperation("查询历史赛季列表")
    @GetMapping("/list")
    public List<PointsBoardSeason> getHistorySeasonList(){
        return pointsBoardSeasonService.getHistorySeasonList();
    }

}
