package com.tianji.promotion.mapper;

import com.tianji.promotion.pojo.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
public interface CouponMapper extends BaseMapper<Coupon> {
    /**
     * 增加优惠券的发放数量
     * @param couponId
     */
    @Update("update coupon set issue_num = issue_num + 1 where id = #{couponId} and issue_num < total_num")
    void incrIssueNum(@Param("couponId") Long couponId);
}
