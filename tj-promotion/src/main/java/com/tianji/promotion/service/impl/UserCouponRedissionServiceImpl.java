//package com.tianji.promotion.service.impl;
//
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.exceptions.BizIllegalException;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.promotion.enums.CouponStatus;
//import com.tianji.promotion.enums.ExchangeCodeStatus;
//import com.tianji.promotion.enums.ObtainType;
//import com.tianji.promotion.enums.UserCouponStatus;
//import com.tianji.promotion.mapper.CouponMapper;
//import com.tianji.promotion.mapper.UserCouponMapper;
//import com.tianji.promotion.pojo.Coupon;
//import com.tianji.promotion.pojo.ExchangeCode;
//import com.tianji.promotion.pojo.UserCoupon;
//import com.tianji.promotion.service.IExchangeCodeService;
//import com.tianji.promotion.service.IUserCouponService;
//import com.tianji.promotion.utils.CodeUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.aop.framework.AopContext;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
///**
// * <p>
// * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
// * </p>
// *
// * @author yizayu
// * @since 2024-10-21
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class UserCouponRedissionServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {
//    private final CouponMapper couponMapper;
//    private final IExchangeCodeService exchangeCodeService;
//    private final RedissonClient redissonClient;
//
//    /**
//     * 领取优惠券接口
//     *
//     * @param couponId
//     */
//    @Override
//    @Transactional
//    public void receiveCoupon(Long couponId) {
//        //
//        Long userId = UserContext.getUser();
//        log.info("用户{}领取优惠券{}", userId, couponId);
//        //
//        Coupon DBcoupon = couponMapper.selectById(couponId);
//        if (DBcoupon == null||DBcoupon.getStatus()!= CouponStatus.ISSUING||DBcoupon.getObtainWay()==ObtainType.ISSUE) {
//            throw new BadRequestException("优惠券不存在或者已经过期或者需要兑换码");
//        }
//        if(DBcoupon.getIssueNum()>=DBcoupon.getTotalNum()){
//            throw new BadRequestException("优惠券已领完");
//        }
//        //
//        String key="lock:coupon:uid:"+userId.toString();
//        RLock lock = redissonClient.getLock(key);
//        boolean tryLock = lock.tryLock();
//        if(!tryLock){
//            throw new BizIllegalException("请求太频繁");
//        }
//        try {
//            //保存用户优惠券
//            IUserCouponService userCouponServiceProxy = (IUserCouponService) AopContext.currentProxy();//获取代理对象
//            userCouponServiceProxy.createUserCoupon(userId, DBcoupon);
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    /**
//     * 兑换码兑换优惠券接口
//     *
//     * @param code
//     */
//    @Override
//    @Transactional
//    public void exchangeCoupon(String code) {
//        //1校验code是否为空
//        if (StringUtils.isBlank(code)) {
//            throw new BadRequestException("非法参数");
//        }
//        //2解析拿到   redis自增id
//        Long serialId = CodeUtil.parseCode(code);
//        //3判断兑换码是否已经兑换   bitmap
//        boolean result=exchangeCodeService.updateExchangeCodeMark(serialId,true);
//        if(result){
//            throw new BizIllegalException("兑换码已被使用");
//        }
//        try {
//            //4判断兑换码是否存在、
//            ExchangeCode exchangeCode = exchangeCodeService.getById(serialId);
//            if (exchangeCode == null) {
//                throw new BizIllegalException("兑换码不存在");
//            }
//            //5判断是否过期
//            LocalDateTime now = LocalDateTime.now();
//            LocalDateTime expiredTime = exchangeCode.getExpiredTime();
//            if(now.isAfter(expiredTime)){
//                throw new BizIllegalException("兑换码已经过期");
//            }
//            //校验并生成用户券
//            //查询优惠券
//            Coupon coupon = couponMapper.selectById(exchangeCode.getExchangeTargetId());
//            if (coupon == null) {
//                throw new BadRequestException("优惠券不存在");
//            }
//            createUserCoupon(UserContext.getUser(),coupon);
////            //增加发放数量
////            couponMapper.incrIssueNum(coupon.getId());
//            //9更新兑换码生成
//            if(serialId!=null){
//                //修改兑换码此状态
//                exchangeCodeService.lambdaUpdate().set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
//                        .set(ExchangeCode::getUserId,UserContext.getUser())
//                        .eq(ExchangeCode::getId,serialId)
//                        .update();
//            }
//        }catch (Exception e){
//            //重置兑换码
//            exchangeCodeService.updateExchangeCodeMark(serialId,false);
//            throw e;
//        }
//    }
//    @Transactional
//    public void createUserCoupon(Long userId, Coupon DBcoupon) {
//        Integer receivedAmount = lambdaQuery()
//                .eq(UserCoupon::getUserId, userId)
//                .eq(UserCoupon::getCouponId, DBcoupon.getId())
//                .count();
//        if(receivedAmount>=DBcoupon.getUserLimit()){
//            throw new BadRequestException("优惠券达到领取上线");
//        }
//        UserCoupon userCoupon=new UserCoupon();
//        userCoupon.setCouponId(DBcoupon.getId());
//        userCoupon.setUserId(userId);
//        userCoupon.setStatus(UserCouponStatus.UNUSED);
//        LocalDateTime termBeginTime = DBcoupon.getTermBeginTime();
//        LocalDateTime termEndTime = DBcoupon.getTermEndTime();
//        if (termBeginTime == null) {
//            termBeginTime = LocalDateTime.now();
//            termEndTime = termBeginTime.plusDays(DBcoupon.getTermDays());
//        }
//        userCoupon.setTermBeginTime(termBeginTime);
//        userCoupon.setTermEndTime(termEndTime);
//
//        //增加发放数量
//        couponMapper.incrIssueNum(DBcoupon.getId());
//
//        save(userCoupon);
//    }
//}
