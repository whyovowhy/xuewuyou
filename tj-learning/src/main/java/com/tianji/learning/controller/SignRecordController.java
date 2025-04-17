package com.tianji.learning.controller;

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
@Api(tags = "签到相关接口")
@RestController
@RequestMapping("/sign-records")
@RequiredArgsConstructor
public class SignRecordController {

    private final ISignRecordService signRecordservice;

    @PostMapping
    @ApiOperation("签到功能接口")
    public SignResultVO addSignRecords(){
        return signRecordservice.addSignRecords();
    }
    @ApiOperation("查询用户本月签到记录")
    @GetMapping
    public List<Byte> selectMonthSignRecords(){
        return signRecordservice.selectMonthSignRecords();
    }
}