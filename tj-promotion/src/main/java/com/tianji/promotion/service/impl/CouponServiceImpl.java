package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domian.dto.CouponFormDTO;
import com.tianji.promotion.domian.dto.CouponIssueFormDTO;
import com.tianji.promotion.domian.query.CouponQuery;
import com.tianji.promotion.domian.vo.CouponPageVO;
import com.tianji.promotion.domian.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.pojo.Coupon;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.pojo.CouponScope;
import com.tianji.promotion.pojo.UserCoupon;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-21
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {
    private final CategoryClient categoryClient;
    private final CouponScopeServiceImpl couponScopeService;
    private final ExchangeCodeServiceImpl exchangeCodeService;

    private final IUserCouponService userCouponService;

    private final StringRedisTemplate redisTemplate;
    /**
     * 新增优惠券
     *
     * @param dto
     */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        save(coupon);
        //优惠券不存在范围
        if(!dto.getSpecific())return;
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BizIllegalException("优惠券范围不能为空");
        }
        List<CouponScope> couponScopeList=new ArrayList<>(scopes.size());
        List<CategoryBasicDTO> cateInfos = categoryClient.getAllOfOneLevel();
        List<Long> caIds = cateInfos.stream().map(CategoryBasicDTO::getId).collect(Collectors.toList());
        for (Long scope : scopes) {
            CouponScope couponScope = new CouponScope();
            couponScope.setCouponId(coupon.getId());
            couponScope.setBizId(scope);
            boolean contains = caIds.contains(scope);
            couponScope.setType(contains?1:2); //1:课程分类 2:课程

            couponScopeList.add(couponScope);
        }
        couponScopeService.saveBatch(couponScopeList);
    }

    /**
     * 分页查询优惠券
     *
     * @param query
     * @return
     */
    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Page<Coupon> couponPages = lambdaQuery()
                .like(query.getName() != null, Coupon::getName, query.getName())
                .eq(query.getType() != null, Coupon::getDiscountType, query.getType())
                .eq(query.getStatus() != null, Coupon::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon> coupons = couponPages.getRecords();
        if(CollUtils.isEmpty(coupons)){
            return PageDTO.empty(couponPages);
        }
        List<CouponPageVO> couponPageVOS = BeanUtils.copyList(coupons, CouponPageVO.class);
        return PageDTO.of(couponPages, couponPageVOS);
    }

    /**
     * 发放优惠券接口
     *
     * @param dto
     */
    @Override
    @Transactional
    public void beginIssue(CouponIssueFormDTO dto) {
        Long couponId = dto.getId();
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        if(coupon.getStatus()!= CouponStatus.DRAFT&&coupon.getStatus()!=CouponStatus.PAUSE){
            throw new BizIllegalException("优惠券状态不正确");
        }
        if(dto.getTermDays()==null){
            coupon.setTermBeginTime(dto.getTermBeginTime());
            coupon.setTermEndTime(dto.getTermEndTime());
        }else{
            coupon.setTermDays(dto.getTermDays());
        }
        coupon.setIssueEndTime(dto.getIssueEndTime());
        boolean isBeginNow = dto.getIssueBeginTime()==null||!dto.getIssueBeginTime().isAfter(LocalDateTime.now());
        if(isBeginNow){
            coupon.setIssueBeginTime(LocalDateTime.now());
            coupon.setStatus(CouponStatus.ISSUING);
        }else {
            coupon.setIssueBeginTime(dto.getIssueBeginTime());
            coupon.setStatus(CouponStatus.PAUSE);
        }
        updateById(coupon);
        /**
         * 立刻发放写入缓存
         */
        if(isBeginNow){
            cacheCoupon(coupon);
        }

        // 兑换码生成
        if(coupon.getStatus()!=CouponStatus.FINISHED&&coupon.getObtainWay()== ObtainType.ISSUE){
            coupon.setIssueEndTime(coupon.getIssueEndTime());
            exchangeCodeService.asyncGenerateCode(coupon);
        }
    }

    /**
     * 缓存优惠券
     * @param coupon
     */
    private void cacheCoupon(Coupon coupon) {
        Map<String,String> couponInfo=new HashMap<>();
        couponInfo.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
        couponInfo.put("issueEndTime",String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
        couponInfo.put("totalNum",coupon.getTotalNum().toString());
        couponInfo.put("issueNum",coupon.getIssueNum().toString());
        couponInfo.put("userLimit",coupon.getUserLimit().toString());

        String key= PromotionConstants.COUPON_CACHE_KEY_PREFIX+coupon.getId();
        redisTemplate.opsForHash().putAll(key,couponInfo);
    }

    /**
     * 修改优惠券
     *
     * @param dto
     * @param id
     */
    @Override
    @Transactional
    public void updateCoupon(CouponFormDTO dto, Long id) {
        Coupon DBcoupon = getById(id);
        if (DBcoupon == null) {
            throw new BizIllegalException("优惠券不存在");
        }
        if (DBcoupon.getStatus() != CouponStatus.DRAFT) {
            throw new BizIllegalException("优惠券状态不正确");
        }
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        coupon.setId(id);
        updateById(coupon);
        List<CouponScope> couponScopeList = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, id).list();
        List<Long> csIds = couponScopeList.stream().map(CouponScope::getId).collect(Collectors.toList());
        couponScopeService.removeByIds(csIds);

        if (dto.getSpecific() && CollUtils.isNotEmpty(dto.getScopes())) {
            List<Long> scopes = dto.getScopes();
            List<CouponScope> couponScopes = scopes.stream().map(bizId -> {
                CouponScope couponScope = new CouponScope();
                couponScope.setBizId(bizId);
                couponScope.setCouponId(id);
                couponScope.setType(1);
                return couponScope;
            }).collect(Collectors.toList());

            couponScopeService.saveBatch(couponScopes);
        }
    }

    /**
     * 查询发放中的优惠券列表
     *
     * @return
     */
    @Override
    public List<CouponVO> queryIssuingCoupons() {
        //
        Long userId = UserContext.getUser();
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        //
        List<Long> cIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> userCoupons = userCouponService
                .lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .in(UserCoupon::getId,cIds)
                .list();
        //
        Map<Long, Long> unUseAmountMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        //
        Map<Long, Long> ucAmountMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        List<CouponVO> couponVOList=new ArrayList<>(coupons.size());
        for (Coupon coupon : coupons) {
            CouponVO couponVO = BeanUtils.copyBean(coupon, CouponVO.class);
            couponVO.setReceived(unUseAmountMap.getOrDefault(coupon.getId(),0L)>0L);//是否可以使用
            couponVO.setAvailable(
                    ucAmountMap.getOrDefault(coupon.getId(),0L)<coupon.getUserLimit()
                    && coupon.getIssueNum()<coupon.getTotalNum());// 是否可领取
            couponVOList.add(couponVO);
        }
        return couponVOList;
    }
}
