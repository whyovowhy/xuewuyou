package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.pojo.InteractionQuestion;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.pojo.InteractionReply;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-11
 */
@Service

public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {
    @Autowired
    private UserClient userClient;
    @Autowired
    private InteractionReplyServiceImpl replyService;
    /**
     * 新增提问
     *
     * @param questionDTO
     */
    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        //1获取用户
        Long userId = UserContext.getUser();

        InteractionQuestion interactionQuestion = BeanUtils.copyBean(questionDTO, InteractionQuestion.class);
        interactionQuestion.setUserId(userId);
        save(interactionQuestion);
    }

    /**
     * 修改提问
     *
     * @param dto
     * @param id
     */
    @Override
    public void updateInteractionQuestion(QuestionFormDTO dto, Long id) {
        //获取提问
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new RuntimeException("提问不存在");
        }
        //校验参数
        if(dto.getAnonymity()==null|| StringUtils.isAllBlank(dto.getDescription(), dto.getTitle())){
            throw new RuntimeException("参数错误");
        }
        //修改
        question.setAnonymity(dto.getAnonymity())
                .setDescription(dto.getDescription())
                .setTitle(dto.getTitle());
        updateById(question);
    }

    /**
     * 分页查询互动问题
     *
     * @param query
     * @return
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        //1获取用户
        Long userId = UserContext.getUser();
        //2分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .eq(query.getOnlyMine()!=null&&query.getOnlyMine(), InteractionQuestion::getUserId, userId)
                .eq(InteractionQuestion::getSectionId, query.getSectionId())
                .eq(InteractionQuestion::getCourseId, query.getCourseId())
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> questions = page.getRecords();
        if (CollUtils.isEmpty(questions)) {
            return PageDTO.empty(page);
        }
        //3获取回复信息
        Set<Long> latestAnswerIds = questions
                .stream()
                .filter(question -> question.getLatestAnswerId() != null)
                .map(InteractionQuestion::getLatestAnswerId)
                .collect(Collectors.toSet());
        Map<Long, InteractionReply> replyMap = new HashMap<>();
        if (CollUtils.isNotEmpty(latestAnswerIds)) {
            List<InteractionReply> replies = replyService
                    .lambdaQuery()
                    .eq(InteractionReply::getHidden, false)
                    .in(InteractionReply::getId, latestAnswerIds)
                    .list();
            replyMap = replies.stream().collect(Collectors.toMap(InteractionReply::getId, reply -> reply));
        }
        //4获取用户信息
        List<Long> userIds = questions
                .stream()
                .filter(question -> !question.getAnonymity())
                .map(InteractionQuestion::getUserId)
                .distinct()
                .collect(Collectors.toList());

        //4.1追加用户id
        for (InteractionReply reply : replyMap.values()) {
            if (!reply.getAnonymity()) {
                userIds.add(reply.getUserId());
            }
        }
        List<UserDTO> userInfos = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userInfoMap = new HashMap<>();
        if (CollUtils.isNotEmpty(latestAnswerIds)) {
            userInfoMap = userInfos.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));
        }
        //5封装返回值
        List<QuestionVO> voList = new ArrayList<>();
        for (InteractionQuestion question : questions) {
            QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
            if (!question.getAnonymity()) {
                UserDTO userDTO = userInfoMap.get(question.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getUsername());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            InteractionReply reply = replyMap.get(question.getLatestAnswerId());
            if (reply != null) {
                if (!reply.getAnonymity()) {
                    UserDTO replyUser = userInfoMap.get(reply.getUserId());
                    if (replyUser != null) {
                        vo.setLatestReplyUser(replyUser.getUsername());
                    }
                }
                vo.setLatestReplyContent(reply.getContent());
            }
            voList.add(vo);
        }
        return PageDTO.of(page,voList);
    }

    /**
     * 根据id查询问题详情
     *
     * @param id
     * @return
     */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        //1查询问题信息
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        if (question.getHidden()){
            return null;
        }
        //2获取用户信息
        Long userId = question.getUserId();
        UserDTO userInfo=new UserDTO();
        if(!question.getAnonymity()){
             userInfo = userClient.queryUserById(userId);
        }
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (userInfo!=null) {
            vo.setUserIcon(userInfo.getIcon());
            vo.setUserName(userInfo.getUsername());
        }
        //3封装返回值
        return vo;
    }

    /**
     * 删除提问
     *
     * @param id
     * @return
     */
    @Override
    public boolean deleteMyQuestionById(Long id) {
        //1查询问题信息
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        //2判断是否为提问者
        Long userId = UserContext.getUser();
        if(!question.getUserId().equals(userId)){
            throw new BadRequestException("不是提问者，无权限删除");
        }
        //3删除问题和回复
        boolean removeQ = removeById(id);
        LambdaQueryWrapper<InteractionReply> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(InteractionReply::getQuestionId,id);
        boolean removeR = replyService.remove(wrapper);

        return removeQ&&removeR;
    }

    /**
     * 管理端分页查询互动问题
     *
     * @param query
     * @return
     */
    @Autowired
    private SearchClient searchClient;
    @Autowired
    private CourseClient courseClient;
    @Autowired
    private CatalogueClient catalogueClient;
    @Autowired
    private CategoryCache categoryCache;
    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        //1根据 query.getCourseName() 查询课程id
        String courseName = query.getCourseName();
        List<Long> cIds=null;
        if (StringUtils.isNotBlank(courseName)) {
            cIds= searchClient.queryCoursesIdByName(query.getCourseName());
            if(CollUtils.isEmpty(cIds)){
                return PageDTO.empty(0L,0L);
            }
        }
        //2根据   cIds    分页查询
        Page<InteractionQuestion> interactionQuestionPage = lambdaQuery()
                .eq(query.getStatus()!=null,InteractionQuestion::getStatus, query.getStatus())
                .gt(query.getBeginTime()!=null,InteractionQuestion::getCreateTime, query.getBeginTime())
                .lt(query.getEndTime()!=null,InteractionQuestion::getCreateTime, query.getEndTime())
                .in(cIds!=null,InteractionQuestion::getCourseId, cIds)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> interactionQuestionList = interactionQuestionPage.getRecords();
        if (CollUtils.isEmpty(interactionQuestionList)) {
            return PageDTO.empty(0L,0L);
        }
        //3根据   interactionQuestionList 获取用户id--->>>用户昵称
        Set<Long> uIds = interactionQuestionList.stream().map(InteractionQuestion::getUserId).collect(Collectors.toSet());
        List<UserDTO> uInfos = userClient.queryUserByIds(uIds);
        if (CollUtils.isEmpty(uInfos)) {
            throw new BizIllegalException("用户不存在");
        }
        Map<Long, UserDTO> uInfoMap = uInfos.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        //4根据   interactionQuestionList 获取cIds--->>>courseName
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfos)) {
            throw new BizIllegalException("课程不存在");
        }
        Map<Long, CourseSimpleInfoDTO> cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        //5   interactionQuestionList 获取chapterId    sectionId   ----->>>>    chapterName  sectionName
        Set<Long> chaIds = interactionQuestionList.stream().map(InteractionQuestion::getChapterId).collect(Collectors.toSet());
        Set<Long> sectIds = interactionQuestionList.stream().map(InteractionQuestion::getSectionId).collect(Collectors.toSet());
        Set<Long> cataIds=new HashSet<>();
        cataIds.addAll(chaIds);
        cataIds.addAll(sectIds);
        List<CataSimpleInfoDTO> cataInfos = catalogueClient.batchQueryCatalogue(cataIds);
        if(CollUtils.isEmpty(cataInfos)){
            throw new BizIllegalException("章节信息不存在");
        }
        Map<Long, String> cataInfoMap = cataInfos.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        //6     categoryName
        //7  封装数据
        List<QuestionAdminVO> voList=new ArrayList<>();
        for (InteractionQuestion record : interactionQuestionList) {
            QuestionAdminVO vo = BeanUtils.copyBean(record, QuestionAdminVO.class);
            //userName
            UserDTO userDTO = uInfoMap.get(record.getUserId());
            if(userDTO!=null){
                vo.setUserName(userDTO.getName());
            }
            //courseName
            CourseSimpleInfoDTO cInfo = cInfoMap.get(record.getCourseId());
            if(cInfo!=null){
                vo.setCourseName(cInfo.getName());
                //categoryName
                String categoryNames = categoryCache.getCategoryNames(cInfo.getCategoryIds());
                vo.setCategoryName(categoryNames);
            }
            //chapterName  sectionName
            vo.setSectionName(cataInfoMap.get(record.getSectionId()));
            vo.setChapterName(cataInfoMap.get(record.getChapterId()));

            voList.add(vo);
        }
        return PageDTO.of(interactionQuestionPage,voList);
        //   chapterName  sectionName   categoryName   userName   courseName
    }

    /**
     * 管理端显示/隐藏问题
     *
     * @param id
     * @param hidden
     * @return
     */
    @Override
    public boolean updateHiddenById(Long id, Boolean hidden) {
        // 查询问题是否存在
        InteractionQuestion question = getById(id);
        if (question == null){
            throw new BadRequestException("问题不存在");
        }
        // 更新隐藏字段
        question.setHidden(hidden);
        return this.updateById(question);
    }
}
