package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.pojo.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-11
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {
    /**
     * 新增提问
     * @param questionDTO
     */
    void saveQuestion(QuestionFormDTO questionDTO);

    /**
     * 修改提问
     * @param dto
     * @param id
     */
    void updateInteractionQuestion(QuestionFormDTO dto, Long id);

    /**
     * 分页查询互动问题
     * @param query
     * @return
     */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    /**
     * 根据id查询问题详情
     * @param id
     * @return
     */
    QuestionVO queryQuestionById(Long id);

    /**
     * 删除提问
     * @param id
     * @return
     */
    boolean deleteMyQuestionById(Long id);

    /**
     * 管理端分页查询互动问题
     * @param query
     * @return
     */
    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

    /**
     * 管理端显示/隐藏问题
     * @param id
     * @param hidden
     * @return
     */
    boolean updateHiddenById(Long id, Boolean hidden);
}
