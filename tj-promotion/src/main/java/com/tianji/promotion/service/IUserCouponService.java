package com.tianji.promotion.service;

import com.tianji.promotion.domian.dto.UserCouponDTO;
import com.tianji.promotion.pojo.Coupon;
import com.tianji.promotion.pojo.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
public interface IUserCouponService extends IService<UserCoupon> {
    /**
     * 领取优惠券接口
     * @param couponId
     */
    void receiveCoupon(Long couponId);

    /**
     * 兑换码兑换优惠券接口
     * @param code
     */
    void exchangeCoupon(String code);

    void createUserCoupon(Long userId, Coupon DBcoupon);

    /**
     * 创建用户优惠券MQ
     * @param userCouponDTO
     */
    void createUserMqCoupon(UserCouponDTO userCouponDTO);
}
