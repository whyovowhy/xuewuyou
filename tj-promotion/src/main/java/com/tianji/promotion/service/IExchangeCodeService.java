package com.tianji.promotion.service;

import com.tianji.promotion.pojo.Coupon;
import com.tianji.promotion.pojo.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {
    /**
     * 兑换码生成
     * @param coupon
     */
    void asyncGenerateCode(Coupon coupon);

    /**
     * 判断兑换码有效性
     * @param serialId
     * @param b
     * @return
     */
    boolean updateExchangeCodeMark(Long serialId, boolean b);
}
