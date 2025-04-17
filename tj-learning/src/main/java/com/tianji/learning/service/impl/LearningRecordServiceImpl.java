package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.pojo.LearningLesson;
import com.tianji.learning.pojo.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-04
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {
    private final ILearningLessonService learningLessonService;
    private final CourseClient courseClient;

    private final LearningRecordDelayTaskHandler delayTaskHandler;
    /**
     * 查询指定课程的学习记录
     *
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        //1获取用户信息
        Long userId = UserContext.getUser();
        //2查询课表
        LearningLesson learningLesson = learningLessonService
                .lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId).one();
        LocalDateTime now = LocalDateTime.now();
        if(learningLesson == null||learningLesson.getExpireTime().isBefore(now)){
            return null;
        }
        //3查询学习记录
        List<LearningRecord> learningRecords = lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLessonId, learningLesson.getId())
                .list();
        //4封装数据
        List<LearningRecordDTO> learningRecordDTOS = BeanUtils.copyList(learningRecords, LearningRecordDTO.class);

        LearningLessonDTO dto=new LearningLessonDTO();
        dto.setRecords(learningRecordDTOS);
        dto.setId(learningLesson.getId());
        if(learningLesson.getLatestSectionId()!=null){
            dto.setLatestSectionId(learningLesson.getLatestSectionId());
        }
        return dto;
    }

    /**
     * 提交学习记录
     *
     * @param formDTO
     */
    @Override
    public void addLearningRecord(LearningRecordFormDTO formDTO) {
        //1获取用户信息
        Long userId = UserContext.getUser();
        //2判断课表是否存在
        LearningLesson lesson = learningLessonService.lambdaQuery()
                .eq(LearningLesson::getId, formDTO.getLessonId())
                .eq(LearningLesson::getUserId, userId)
                .one();
        if(lesson == null){
            throw new BadRequestException("课程不存在");
        }
        //3判断课程是否在有效期内
        if(lesson.getExpireTime().isBefore(LocalDateTime.now())||lesson.getStatus()==LessonStatus.EXPIRED) {
            throw new BadRequestException("课程已过期");
        }
        //4判断学习类型     操作学习记录
        Boolean isFirstFinished=false;
        if(formDTO.getSectionType() == SectionType.VIDEO){//视频
            isFirstFinished=handleVideo(formDTO, userId);
        }else if(formDTO.getSectionType() == SectionType.EXAM){//考试
            isFirstFinished=handleExam(formDTO, userId);
        }else {
            throw new BadRequestException("学习类型错误");
        }
        //5更新课表信息
        if(isFirstFinished){
            updateLearningLesson(lesson,formDTO);
        }
    }

    /**
     * 更新课表信息
     * @param
     * @param lesson
     * @param formDTO
     */
    private void updateLearningLesson(LearningLesson lesson, LearningRecordFormDTO formDTO) {
        Boolean allFinished=false;
        //是否完成该小节内容
        //判断是否完成所有小节
        CourseFullInfoDTO cInfo =
                courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if(cInfo == null){
            throw new BadRequestException("课程不存在");
        }
        Integer cSectionNum = cInfo.getSectionNum();
        allFinished=lesson.getLearnedSections()+1>=cSectionNum;
        boolean update = learningLessonService.lambdaUpdate()
                .set(lesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(allFinished,LearningLesson::getStatus, LessonStatus.FINISHED)//完成所有小节
                .set(LearningLesson::getLatestLearnTime, formDTO.getCommitTime())
                .set(LearningLesson::getLatestSectionId, formDTO.getSectionId())
                .setSql("learned_sections=learned_sections+1")//第一次完成某小节
                .eq(LearningLesson::getId, lesson.getId())
                .update();
    }

    /**
     * 考试
     *
     * @param formDTO
     * @param userId
     * @return  是否第一次完成该小节内容
     */
    private Boolean handleExam(LearningRecordFormDTO formDTO, Long userId) {
        //新增学习记录
        LearningRecord learningRecord = BeanUtils.copyBean(formDTO, LearningRecord.class);
        learningRecord.setUserId(userId);
        learningRecord.setCreateTime(formDTO.getCommitTime());
        learningRecord.setFinished(true);
        learningRecord.setFinishTime(formDTO.getCommitTime());

        save(learningRecord);
        return true;
    }

    /**
     * 视频
     *
     * @param formDTO
     * @param userId
     * @return  是否第一次完成该小节内容
     */
    private Boolean handleVideo(LearningRecordFormDTO formDTO, Long userId) {
        //1是否存在学习记录
        Boolean isFirstFinished=false;

        LearningRecord oldLearningRecord=selectOldRecord(formDTO.getLessonId(), formDTO.getSectionId());

        if(oldLearningRecord == null){
            //记录不存在，创建记录
            LearningRecord learningRecord = BeanUtils.copyBean(formDTO, LearningRecord.class);
            learningRecord.setCreateTime(formDTO.getCommitTime());
            learningRecord.setUserId(userId);
            save(learningRecord);
        }else {
            //记录存在，更新记录
            // 判断是否完成
            isFirstFinished= !oldLearningRecord.getFinished()&&formDTO.getMoment()*2>= formDTO.getDuration();

            if(!isFirstFinished){
                LearningRecord learningRecord=new LearningRecord();
                learningRecord.setMoment(formDTO.getMoment());
                learningRecord.setLessonId(formDTO.getLessonId());
                learningRecord.setSectionId(formDTO.getSectionId());
                learningRecord.setId(oldLearningRecord.getId());
                learningRecord.setFinished(oldLearningRecord.getFinished());
                delayTaskHandler.addLearningRecordTask(learningRecord);
                return false;
            }

            boolean update = lambdaUpdate()
                    .set(LearningRecord::getMoment, formDTO.getMoment())
                    .set(isFirstFinished, LearningRecord::getFinished, true)
                    .set(isFirstFinished, LearningRecord::getFinishTime, formDTO.getCommitTime())
                    .eq(LearningRecord::getId, oldLearningRecord.getId())
                    .update();
            //清除缓存
            delayTaskHandler.cleanRecordCache(formDTO.getLessonId(), formDTO.getSectionId());
        }
        return isFirstFinished;
    }

    /**
     * 是否存在学习记录
     * @param lessonId
     * @param sectionId
     * @return
     */
    private LearningRecord selectOldRecord(Long lessonId, Long sectionId) {
        //1从缓存中查询是否存在学习记录
        LearningRecord RedisLearningRecord = delayTaskHandler.readRecordCache(lessonId, sectionId);
        if(RedisLearningRecord != null){
            return RedisLearningRecord;
        }
        //2缓存中不存在，从数据库中查询
        LearningRecord DBLearningRecord = lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        //3将查询结果放入缓存
        if(DBLearningRecord != null){
            delayTaskHandler.writeRecordCache(DBLearningRecord);
        }
        return DBLearningRecord;
    }
}
