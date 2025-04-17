package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domian.dto.CouponFormDTO;
import com.tianji.promotion.domian.dto.CouponIssueFormDTO;
import com.tianji.promotion.domian.query.CouponQuery;
import com.tianji.promotion.domian.vo.CouponPageVO;
import com.tianji.promotion.domian.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
@Api(tags = "优惠券相关接口")
public class CouponController {

    private final ICouponService couponService;

    @ApiOperation("新增优惠券")
    @PostMapping
    public void saveCoupon(@RequestBody @Validated CouponFormDTO dto){
        couponService.saveCoupon(dto);
    }
    @ApiOperation("分页查询优惠券接口")
    @GetMapping("/page")
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query){
        return couponService.queryCouponByPage(query);
    }
    @ApiOperation("发放优惠券接口")
    @PutMapping("/{id}/issue")
    public void beginIssue(@RequestBody @Validated CouponIssueFormDTO dto) {
        couponService.beginIssue(dto);
    }
    @ApiOperation("修改优惠券")
    @PutMapping("/{id}")
    public void updateCoupon(@PathVariable("id") Long id,
                             @Validated @RequestBody CouponFormDTO dto) {
        couponService.updateCoupon(dto, id);
    }
    @ApiOperation("查询发放中的优惠券列表")
    @GetMapping("/list")
    public List<CouponVO> queryIssuingCoupons(){
        return couponService.queryIssuingCoupons();
    }
}
