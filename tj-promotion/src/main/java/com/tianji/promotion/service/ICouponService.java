package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domian.dto.CouponFormDTO;
import com.tianji.promotion.domian.dto.CouponIssueFormDTO;
import com.tianji.promotion.domian.query.CouponQuery;
import com.tianji.promotion.domian.vo.CouponPageVO;
import com.tianji.promotion.domian.vo.CouponVO;
import com.tianji.promotion.pojo.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
public interface ICouponService extends IService<Coupon> {
    /**
     * 新增优惠券
     * @param dto
     */
    void saveCoupon(CouponFormDTO dto);

    /**
     * 分页查询优惠券
     * @param query
     * @return
     */
    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);

    /**
     * 发放优惠券接口
     * @param dto
     */
    void beginIssue(CouponIssueFormDTO dto);

    /**
     * 修改优惠券
     * @param dto
     * @param id
     */
    void updateCoupon(CouponFormDTO dto, Long id);

    /**
     * 查询发放中的优惠券列表
     * @return
     */
    List<CouponVO> queryIssuingCoupons();
}
