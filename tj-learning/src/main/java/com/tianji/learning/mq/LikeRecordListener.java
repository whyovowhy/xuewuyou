package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.domain.dto.LikedTimesDTO;
import com.tianji.learning.pojo.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeRecordListener {
    private final IInteractionReplyService replyService;
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(name = "qa.liked.times.queue", durable = "true"),
//            exchange = @Exchange(name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
//            key = MqConstants.Key.QA_LIKED_TIMES_KEY
//    ))
//    public void listenReplyLikedTimesChange(LikedTimesDTO dto) {
//        log.debug("监听到回答或评论{}的点赞数变更:{}", dto.getBizId(), dto.getLikedTimes());
//        InteractionReply reply = new InteractionReply();
//        reply.setId(dto.getBizId());
//        reply.setLikedTimes(dto.getLikedTimes());
//
//        replyService.updateById(reply);
//    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.liked.times.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.QA_LIKED_TIMES_KEY
    ))
    public void listenReplyLikedTimesChange(List<LikedTimesDTO> likedTimesDTOS) {
        log.debug("监听到回答的点赞数变更:{}", likedTimesDTOS);
        List<InteractionReply> replies=new ArrayList<>();
        for (LikedTimesDTO dto : likedTimesDTOS) {
            InteractionReply reply = new InteractionReply();
            reply.setId(dto.getBizId());
            reply.setLikedTimes(dto.getLikedTimes());

            replies.add(reply);
        }
        replyService.updateBatchById(replies);
    }
}
