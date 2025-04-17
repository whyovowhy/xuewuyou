package com.tianji.promotion.service.impl;

import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.pojo.Coupon;
import com.tianji.promotion.pojo.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {
    /**
     * 兑换码生成
     * @param coupon
     */

    /**
     * 异步生成兑换码
     *
     * @param coupon
     */
    private final StringRedisTemplate redisTemplate;
    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        log.debug("生成兑换码  线程----{}",Thread.currentThread().getName());
        Integer totalNum = coupon.getTotalNum();
        log.info("--------------------开始发送优惠券{}的兑换码，数量为{}--------------------", coupon.getId(), totalNum);
        //1生成自增id  redis
        Long increment = redisTemplate.opsForValue().increment(PromotionConstants.COUPON_CODE_SERIAL_KEY, totalNum);
        if(increment==null){
            return;
        }
        //2调用工具生成兑换码
        int maxSerialNum = increment.intValue();
        int begin=maxSerialNum-totalNum+1;
        List<ExchangeCode> list=new ArrayList<>();
        for(int i=begin;i<=maxSerialNum;i++){
            String code = CodeUtil.generateCode(i, coupon.getId());//（自增id，优惠券id）内部算出0-15数字计算出对应密钥
            ExchangeCode exchangeCode=new ExchangeCode();
            exchangeCode.setId(i);//兑换码id
            exchangeCode.setCode(code);
            exchangeCode.setExchangeTargetId(coupon.getId());
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());//兑换码兑换截止时间，优惠券领取的截止时间

            list.add(exchangeCode);
        }
        //3将兑换码保存db
        this.saveBatch(list);


        // 4.写入Redis缓存，member：couponId，score：兑换码的最大序列号
        redisTemplate.opsForZSet().add(PromotionConstants.COUPON_RANGE_KEY, coupon.getId().toString(), maxSerialNum);
        log.info("--------------------{}个优惠券{}的兑换码发送完毕--------------------", totalNum,coupon.getId());
    }

    /**
     * 判断兑换码有效性
     *
     * @param serialId
     * @param b
     * @return
     */
    @Override
    public boolean updateExchangeCodeMark(Long serialId, boolean b) {
        Boolean boo = redisTemplate.opsForValue().setBit(PromotionConstants.COUPON_CODE_MAP_KEY, serialId, b);
        return boo != null && boo;
    }
}
