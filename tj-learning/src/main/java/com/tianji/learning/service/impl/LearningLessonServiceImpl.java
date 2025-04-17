package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.pojo.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.pojo.LearningRecord;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-03
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final LearningRecordMapper recordMapper;
    /**
     * 添加用户课程
     *
     * @param userId
     * @param courseIds
     */
    @Override
    public void addUserLesson(Long userId, List<Long> courseIds) {
        //1远程调用获取课程信息
        List<CourseSimpleInfoDTO> cinfos= courseClient.getSimpleInfoList(courseIds);
        //2封装实体类填充过期时间
        List<LearningLesson> list=new ArrayList<>();
        for (CourseSimpleInfoDTO cinfo : cinfos) {
            LearningLesson lesson=new LearningLesson();
            lesson.setUserId(userId);
            lesson.setCourseId(cinfo.getId());
            Integer validDuration = cinfo.getValidDuration();//课程有效期
            if(validDuration!=null){
                LocalDateTime  expireTime= LocalDateTime.now().plusMonths(validDuration);
                lesson.setCreateTime(LocalDateTime.now());
                lesson.setExpireTime(expireTime);
            }
            list.add(lesson);
        }
        //3保存数据
        saveBatch(list);
    }

    /**
     * 查询我的课表
     *
     * @param query
     * @return
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1获取用户id
        Long userId = UserContext.getUser();
        //2查询我的课表
        Page<LearningLesson> lessonPage =
                lambdaQuery().eq(LearningLesson::getUserId, userId)
                        .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> lessonPageRecords = lessonPage.getRecords();
        if(CollUtils.isEmpty(lessonPageRecords)){
            return PageDTO.empty(lessonPage);
        }
        //3获取课程信息
        Set<Long> cIds = lessonPageRecords.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        if(CollUtils.isEmpty(cInfos)){
            throw new BadRequestException("课程信息不存在");
        }
        Map<Long, CourseSimpleInfoDTO> cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        //4封装返回值
        List<LearningLessonVO>voList=new ArrayList<>();
        for (LearningLesson lesson : lessonPageRecords) {
            LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
            Long courseId = lesson.getCourseId();

            vo.setCourseName(cInfoMap.get(courseId).getName());
            vo.setSections(cInfoMap.get(courseId).getSectionNum());
            vo.setCourseCoverUrl(cInfoMap.get(courseId).getCoverUrl());

            voList.add(vo);
        }
        return PageDTO.of(lessonPage,voList);
    }

    /**
     * 查询我正在学习的课程
     *
     * @return
     */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        //1获取用户id
        Long userId = UserContext.getUser();
        //2查询最近学习的课程
        LearningLesson lesson = lambdaQuery().eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        Long courseId = lesson.getCourseId();
        //3获取课程总数
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        //4获取课程信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(courseId, false, false);
        if(cInfo==null){
            throw new BadRequestException("课程信息不存在");
        }
        //5获取章节信息
        List<CataSimpleInfoDTO> cataInfos = catalogueClient.batchQueryCatalogue(Collections.singletonList(courseId));
        //6封装返回值
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        vo.setCourseAmount(courseAmount);
        vo.setCourseName(cInfo.getName());
        vo.setSections(cInfo.getSectionNum());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        if(CollUtils.isNotEmpty(cataInfos)){
            vo.setLatestSectionName(cataInfos.get(0).getName());
            vo.setLatestSectionIndex(cataInfos.get(0).getCIndex());
        }
        return vo;
    }

    /**
     * 判断课程是否有效
     *
     * @param courseId
     * @return
     */
    @Override
    public Long isLessonValid(Long courseId) {
        if(courseId==null){
            throw new BadRequestException("课程id为空");
        }
        //1获取用户id
        Long userId = UserContext.getUser();
        //2判断课程是否有效
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(lesson==null){
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if(lesson.getExpireTime()!=null&&lesson.getExpireTime().isAfter(now)){
            return lesson.getId();
        }
        return null;
    }

    /**
     * 查询用户课表中指定课程状态
     *
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonVO getLessonInfo(Long courseId) {
        //1获取用户id
        Long userId = UserContext.getUser();
        //2查询课程信息
        if(courseId==null){
//            throw new BadRequestException("课程id为空");
            return null;
        }
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(lesson==null){
//            throw new BadRequestException("课程不存在");
            return null;
        }
        //3封装返回值
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        return vo;
    }

    /**
     * 创建学习计划
     *
     * @param planDTO
     */
    @Override
    public void createLearningPlan(LearningPlanDTO planDTO) {
        if(ObjectUtils.isEmpty(planDTO)){
            throw new BadRequestException("学习计划为空");
        }
        //1获取用户id
        Long userId = UserContext.getUser();
        //2判断课程是否有效
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, planDTO.getCourseId())
                .one();
        if(lesson==null){
            throw new BadRequestException("课程不存在");
        }
        lesson.setStatus(LessonStatus.LEARNING);
        lesson.setPlanStatus(PlanStatus.PLAN_RUNNING);
        lesson.setWeekFreq(planDTO.getFreq());
        //3更新学习计划
        updateById(lesson);
    }

    /**
     * 查询我的学习计划
     *
     * @param query
     * @return
     */
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        //1获取用户id
        Long userId = UserContext.getUser();
        //2查询课表
        Page<LearningLesson> lessonPage = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> lessons = lessonPage.getRecords();
        if(CollUtils.isEmpty(lessons)){
        }
        //2.1获取本周计划小节数量
        int weekTotalPlan = lessons.stream().mapToInt(LearningLesson::getWeekFreq).sum();
        //3获取课表课程
        Set<Long> cIds = lessons.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        //3.1获取各个课程名称
        Map<Long, String> cNameMap =
                cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
        //3.2获取各个课程小节数量
        Map<Long, Integer> cSectionNumMap =
                cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getSectionNum));
        //4获取各个课程本周已学习小节数量
        Set<Long> lIds = lessons.stream().map(LearningLesson::getId).collect(Collectors.toSet());
        //4.1获取本周开始时间和结束时间
        LocalDate now = LocalDate.now();
        LocalDateTime weekBeginTime = DateUtils.getWeekBeginTime(now);
        LocalDateTime weekEndTime = DateUtils.getWeekEndTime(now);

        List<LearningRecord> learningRecords = recordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                        .in(LearningRecord::getLessonId, lIds)
                        .eq(LearningRecord::getFinished, true)
                        .gt(LearningRecord::getFinishTime, weekBeginTime)
                        .lt(LearningRecord::getFinishTime, weekEndTime)
        );

        Map<Long, List<LearningRecord>> lRcordsMap =
                learningRecords.stream().collect(Collectors.groupingBy(LearningRecord::getLessonId));
        //4.2获取本周所有课程已学习小节数量
        int weekFinished = learningRecords.size();

        List<LearningPlanVO> learningPlanVOS = new ArrayList<>(lessons.size());

        for(LearningLesson lesson : lessons){
            LearningPlanVO planVO = BeanUtils.copyBean(lesson, LearningPlanVO.class);
            planVO.setWeekLearnedSections(0);
            if(CollUtils.isNotEmpty(lRcordsMap.get(planVO.getId()))){
                planVO.setWeekLearnedSections(lRcordsMap.get(planVO.getId()).size());
            }
            planVO.setSections(cSectionNumMap.get(planVO.getCourseId()));
            planVO.setCourseName(cNameMap.get(planVO.getCourseId()));

            learningPlanVOS.add(planVO);
        }
//        for (LearningPlanVO planVO : learningPlanVOS) {
//            if(CollUtils.isNotEmpty(lRcordsMap.get(planVO.getId()))){
//                planVO.setWeekLearnedSections(lRcordsMap.get(planVO.getId()).size());
//            }
//            planVO.setSections(cSectionNumMap.get(planVO.getCourseId()));
//            planVO.setCourseName(cNameMap.get(planVO.getCourseId()));
//
//            learningPlanVOS.add(planVO);
//        }


//         TODO: 2024/10/4 查询本周学习积分
        Integer weekPoints=9;

        //5封装返回值
        LearningPlanPageVO planPageVO = new LearningPlanPageVO();
        planPageVO.setWeekPoints(weekPoints);
        planPageVO.setWeekTotalPlan(weekTotalPlan);
        planPageVO.setWeekFinished(weekFinished);
        planPageVO.pageInfo(lessonPage.getTotal(),lessonPage.getPages(),learningPlanVOS);

        return planPageVO;
    }
}
