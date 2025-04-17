package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.pojo.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-03
 */
public interface ILearningLessonService extends IService<LearningLesson> {
    /**
     * 添加用户课程
     * @param userId
     * @param courseIds
     */
    void addUserLesson(Long userId, List<Long> courseIds);

    /**
     * 查询我的课表
     * @param query
     * @return
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    /**
     * 查询我正在学习的课程
     * @return
     */
    LearningLessonVO queryMyCurrentLesson();

    /**
     * 判断课程是否有效
     * @param courseId
     * @return
     */
    Long isLessonValid(Long courseId);

    /**
     * 查询用户课表中指定课程状态
     * @param courseId
     * @return
     */
    LearningLessonVO getLessonInfo(Long courseId);

    /**
     * 创建学习计划
     * @param planDTO
     */
    void createLearningPlan(LearningPlanDTO planDTO);

    /**
     * 查询我的学习计划
     * @param query
     * @return
     */
    LearningPlanPageVO queryMyPlans(PageQuery query);
}
