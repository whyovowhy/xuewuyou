package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author yizayu
 * @since 2024-10-11
 */
@Api(tags = "互动提问的评论接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/replies")
public class InteractionReplyController {

    private final IInteractionReplyService interactionReplyService;
    @ApiOperation("新增回答或评论")
    @PostMapping
    public void addReply(@RequestBody ReplyDTO replyDTO){
        interactionReplyService.addReply(replyDTO);
    }
    @ApiOperation("分页查询回答或评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery pageQuery){
        return interactionReplyService.pageUser(pageQuery);
    }
}
