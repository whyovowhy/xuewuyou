package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author yizayu
 * @since 2024-10-11
 */
@RestController
@RequestMapping("/admin/questions")
@Api(tags = "互动问答管理端相关接口")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService questionService;
    private final IInteractionReplyService replyService;

    @ApiOperation("管理端分页查询互动问题")
    @GetMapping("page")
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query){
        return questionService.queryQuestionPageAdmin(query);
    }
    @ApiOperation("管理端显示/隐藏问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public boolean updateHiddenById(@PathVariable("id") Long id,
                                    @PathVariable("hidden") Boolean hidden) {
        return questionService.updateHiddenById(id, hidden);
    }
    @ApiOperation("管理端分页查询回答或评论列表")
    @GetMapping("/replies/page")
    public PageDTO<ReplyVO> page(ReplyPageQuery pageQuery){
        return replyService.pageAdmin(pageQuery);
    }
}