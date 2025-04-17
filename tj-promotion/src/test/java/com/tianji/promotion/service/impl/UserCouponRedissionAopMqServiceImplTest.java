//package com.tianji.promotion.service.impl;
//
//import cn.hutool.core.bean.copier.CopyOptions;
//import com.tianji.common.exceptions.BizIllegalException;
//import com.tianji.common.utils.BeanUtils;
//import com.tianji.promotion.constants.PromotionConstants;
//import com.tianji.promotion.pojo.Coupon;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.StringRedisTemplate;
//
//import java.util.Map;
//
//
//@SpringBootTest
//class UserCouponRedissionAopMqServiceImplTest {
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//    @Test
//    public void receiveCoupon() {
//        //1从redis获取优惠券信息
//        Map<Object, Object> couponInfo =
//                redisTemplate.opsForHash().entries(PromotionConstants.COUPON_CACHE_KEY_PREFIX + "1849339207223885826");
//        if (couponInfo == null) {
//            throw new BizIllegalException("优惠券不存在");
//        }
//        Coupon coupon = BeanUtils.mapToBean(couponInfo, Coupon.class, false, CopyOptions.create());
//        if (coupon == null) {
//            throw new BizIllegalException("优惠券不存在");
//        }
//        System.out.println(coupon);
//    }
//}