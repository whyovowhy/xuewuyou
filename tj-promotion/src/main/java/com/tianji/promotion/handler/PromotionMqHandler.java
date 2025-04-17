package com.tianji.promotion.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.promotion.domian.dto.UserCouponDTO;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PromotionMqHandler {
    private final IUserCouponService userCouponService;


    /**
     * 更新优惠券已发放数量，新增用户券
     * @param userCouponDTO   这里需要用到优惠券id和用户id
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "coupon.receive.queue",durable = "true"),
            exchange = @Exchange(name =MqConstants.Exchange.PROMOTION_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COUPON_RECEIVE
    ))
    public void receiveCoupon(UserCouponDTO userCouponDTO) {
        userCouponService.createUserMqCoupon(userCouponDTO);
    }
}
