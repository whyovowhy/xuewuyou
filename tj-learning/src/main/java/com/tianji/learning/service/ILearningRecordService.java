package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.pojo.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-04
 */
public interface ILearningRecordService extends IService<LearningRecord> {
    /**
     * 查询指定课程的学习记录
     * @param courseId
     * @return
     */
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    /**
     * 提交学习记录
     * @param formDTO
     */
    void addLearningRecord(LearningRecordFormDTO formDTO);
}
