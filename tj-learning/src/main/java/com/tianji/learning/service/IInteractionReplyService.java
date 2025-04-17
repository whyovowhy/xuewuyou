package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.pojo.InteractionReply;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 互动问题的回答或评论 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-11
 */
public interface IInteractionReplyService extends IService<InteractionReply> {
    /**
     * 新增回答或评论
     * @param replyDTO
     */
    void addReply(ReplyDTO replyDTO);

    /**
     * 分页查询回答或评论列表
     * @param pageQuery
     * @return
     */
    PageDTO<ReplyVO> pageUser(ReplyPageQuery pageQuery);

    /**
     * 管理端分页查询回答或评论列表
     * @param pageQuery
     * @return
     */
    PageDTO<ReplyVO> pageAdmin(ReplyPageQuery pageQuery);
}
