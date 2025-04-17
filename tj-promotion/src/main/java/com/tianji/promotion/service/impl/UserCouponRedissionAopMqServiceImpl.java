package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.anno.MyLock;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domian.dto.UserCouponDTO;
import com.tianji.promotion.enums.*;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.pojo.Coupon;
import com.tianji.promotion.pojo.ExchangeCode;
import com.tianji.promotion.pojo.UserCoupon;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCouponRedissionAopMqServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
    private final CouponMapper couponMapper;
    private final IExchangeCodeService exchangeCodeService;
    private final RedissonClient redissonClient;

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 领取优惠券接口
     *
     * @param couponId
     */
    @Override
//    @Transactional
    @MyLock(name = "lock:coupon:uid:",type = MyLockType.RE_ENTRANT_LOCK,strategy = MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT)

    public void receiveCoupon(Long couponId) {
        //1从redis获取优惠券信息
        Map<Object, Object> couponInfo =
                redisTemplate.opsForHash().entries(PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId);
        if (couponInfo == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        Coupon coupon = BeanUtils.mapToBean(couponInfo, Coupon.class, false, CopyOptions.create());
        if(coupon == null){
            throw new BizIllegalException("优惠券不存在");
        }
        //2校验优惠券信息
        if(coupon.getIssueBeginTime().isAfter(LocalDateTime.now())|| coupon.getIssueEndTime().isBefore(LocalDateTime.now())){
            throw new BizIllegalException("优惠券还未开始发放或者已经过期");
        }
        if(coupon.getIssueNum()>= coupon.getTotalNum()){
            throw new BizIllegalException("优惠券已经被领完");
        }
        //2.1校验用户是否已经领取过
        Long userId = UserContext.getUser();
        String key=PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
        Long increment = redisTemplate.opsForHash().increment(key, userId.toString(), 1);
        if (increment > coupon.getUserLimit()) {
            redisTemplate.opsForHash().increment(key, userId.toString(), -1);
            throw new BizIllegalException("超过限领数量");
        }
        //3更新缓存
        redisTemplate.opsForHash().put(PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId, "issueNum", String.valueOf(coupon.getIssueNum() + 1));

        UserCouponDTO userCouponDTO=new UserCouponDTO();
        userCouponDTO.setCouponId(couponId);
        userCouponDTO.setUserId(userId);
        rabbitMqHelper.send(
                MqConstants.Exchange.PROMOTION_EXCHANGE,
                MqConstants.Key.COUPON_RECEIVE,
                userCouponDTO
        );
    }
    /**
     * 兑换码兑换优惠券接口
     *
     * @param code
     */
    @Override
    @Transactional
    public void exchangeCoupon(String code) {
        //1校验code是否为空
        if (StringUtils.isBlank(code)) {
            throw new BadRequestException("非法参数");
        }
        //2解析拿到   redis自增id
        Long serialId = CodeUtil.parseCode(code);
        //3判断兑换码是否已经兑换   bitmap
        boolean result=exchangeCodeService.updateExchangeCodeMark(serialId,true);
        if(result){
            throw new BizIllegalException("兑换码已被使用");
        }
        try {
            //4判断兑换码是否存在、
            ExchangeCode exchangeCode = exchangeCodeService.getById(serialId);
            if (exchangeCode == null) {
                throw new BizIllegalException("兑换码不存在");
            }
            //5判断是否过期
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiredTime = exchangeCode.getExpiredTime();
            if(now.isAfter(expiredTime)){
                throw new BizIllegalException("兑换码已经过期");
            }
            //校验并生成用户券
            //查询优惠券
            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
            if (coupon == null) {
                throw new BadRequestException("优惠券不存在");
            }
            createUserCoupon(UserContext.getUser(),coupon);
//            //增加发放数量
//            couponMapper.incrIssueNum(coupon.getId());
            //9更新兑换码生成
            if(serialId!=null){
                //修改兑换码此状态
                exchangeCodeService.lambdaUpdate().set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                        .set(ExchangeCode::getUserId,UserContext.getUser())
                        .eq(ExchangeCode::getId,serialId)
                        .update();
            }
        }catch (Exception e){
            //重置兑换码
            exchangeCodeService.updateExchangeCodeMark(serialId,false);
            throw e;
        }
    }

    /**
     * 创建用户优惠券MQ
     *
     * @param userCouponDTO
     */
    @Override
    @Transactional
    public void createUserMqCoupon(UserCouponDTO userCouponDTO) {
        //查询优惠券
        Coupon DBcoupon = couponMapper.selectById(userCouponDTO.getCouponId());
        if (DBcoupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        //校验并生成用户券
        UserCoupon userCoupon=new UserCoupon();
        userCoupon.setCouponId(DBcoupon.getId());
        userCoupon.setUserId(userCouponDTO.getUserId());
        userCoupon.setStatus(UserCouponStatus.UNUSED);
        LocalDateTime termBeginTime = DBcoupon.getTermBeginTime();
        LocalDateTime termEndTime = DBcoupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(DBcoupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);

        //增加发放数量
        couponMapper.incrIssueNum(DBcoupon.getId());

        save(userCoupon);
    }

    @Transactional
    @Override
    public void createUserCoupon(Long userId, Coupon DBcoupon) {
        Integer receivedAmount = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, DBcoupon.getId())
                .count();
        if(receivedAmount>=DBcoupon.getUserLimit()){
            throw new BadRequestException("优惠券达到领取上线");
        }
        UserCoupon userCoupon=new UserCoupon();
        userCoupon.setCouponId(DBcoupon.getId());
        userCoupon.setUserId(userId);
        userCoupon.setStatus(UserCouponStatus.UNUSED);
        LocalDateTime termBeginTime = DBcoupon.getTermBeginTime();
        LocalDateTime termEndTime = DBcoupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(DBcoupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);

        //增加发放数量
        couponMapper.incrIssueNum(DBcoupon.getId());

        save(userCoupon);
    }
}
