package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.pojo.InteractionQuestion;
import com.tianji.learning.pojo.InteractionReply;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-11
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {
    private final InteractionQuestionMapper questionMapper;
    private final UserClient userClient;
    /**
     * 新增回答或评论
     *
     * @param replyDTO
     */
    @Override
    @Transactional
    public void addReply(ReplyDTO replyDTO) {
        //1获取当前用户
        Long userId = UserContext.getUser();
        //2保存回答或评论
        InteractionReply interactionReply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        interactionReply.setUserId(userId);
        save(interactionReply);
        //3判断是否是回答
        InteractionQuestion interactionQuestion=questionMapper.selectById(replyDTO.getQuestionId());
        if (replyDTO.getAnswerId() == null) {//回答
            interactionQuestion.setLatestAnswerId(interactionReply.getId());
            interactionQuestion.setAnswerTimes(interactionQuestion.getAnswerTimes() + 1);
        }else {//评论
            InteractionReply reply = getById(replyDTO.getAnswerId());//回答
            reply.setReplyTimes(reply.getReplyTimes() + 1);
            updateById(reply);
        }
        if(replyDTO.getIsStudent()){//学生
            //更新问题状态
            interactionQuestion.setStatus(QuestionStatus.UN_CHECK);
        }
        questionMapper.updateById(interactionQuestion);
    }

    /**
     * 分页查询回答或评论列表
     *
     * @param pageQuery
     * @return
     */
    @Override
    public PageDTO<ReplyVO> pageUser(ReplyPageQuery pageQuery) {
        if(pageQuery.getAnswerId()==null&&pageQuery.getQuestionId()==null){
            throw new BadRequestException("参数错误");
        }
        Page<InteractionReply> replyPage = lambdaQuery()
                .eq(InteractionReply::getAnswerId, pageQuery.getAnswerId() == null ? 0 : pageQuery.getAnswerId())
                .eq(pageQuery.getQuestionId() != null, InteractionReply::getQuestionId, pageQuery.getQuestionId())
                .page(pageQuery.toMpPage(new OrderItem(Constant.DATA_FIELD_NAME_LIKED_TIME,false),
                        new OrderItem(Constant.DATA_FIELD_NAME_CREATE_TIME,true)));
        List<InteractionReply> replies = replyPage.getRecords();
        if(CollUtils.isEmpty(replies)){
            return PageDTO.empty(replyPage);
        }
        Set<Long> userIds = replies
                .stream()
                .filter(reply->!reply.getAnonymity())
                .map(InteractionReply::getUserId)
                .collect(Collectors.toSet());
        Set<Long> targetUserIds=replies
                .stream()
                .filter(reply->reply.getTargetUserId()!=null&&reply.getTargetUserId()>0)
                .map(InteractionReply::getTargetUserId)
                .collect(Collectors.toSet());
        Set<Long> targetReplyIds=replies
                .stream()
                .filter(reply->reply.getTargetReplyId()!=null&&reply.getTargetReplyId()>0)
                .map(InteractionReply::getTargetReplyId)
                .collect(Collectors.toSet());
        //获取用户信息
        Map<Long, UserDTO> uInfoMap = userClient.queryUserByIds(userIds)
                .stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        Map<Long, UserDTO> tuInfoMap = userClient.queryUserByIds(targetUserIds)
                .stream().collect((Collectors.toMap(UserDTO::getId, c -> c)));
        //获取目标回答信息
        Map<Long, InteractionReply> tReplyInfoMap=new HashMap<>();
        if(CollUtils.isNotEmpty(targetReplyIds)){
            tReplyInfoMap = listByIds(targetReplyIds)
                    .stream().collect(Collectors.toMap(InteractionReply::getId, c -> c));
        }
        //封装结果
        List<ReplyVO> voList=new ArrayList<>();
        for (InteractionReply reply : replies) {
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
            UserDTO userDTO = uInfoMap.get(reply.getUserId());
            if(userDTO!=null&&!vo.getAnonymity()){
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
                vo.setUserType(userDTO.getType());
            }
            UserDTO tuserInfo = tuInfoMap.get(reply.getTargetUserId());
            InteractionReply tReply = tReplyInfoMap.get(reply.getTargetReplyId());
            if(tuserInfo!=null&&tReply!=null&&!tReply.getAnonymity()){
                vo.setTargetUserName(tuserInfo.getName());
            }
            vo.setLiked(reply.getLikedTimes()>0);
            voList.add(vo);
        }
        return PageDTO.of(replyPage,voList);
    }

    /**
     * 管理端分页查询回答或评论列表
     *
     * @param pageQuery
     * @return
     */
    @Override
    public PageDTO<ReplyVO> pageAdmin(ReplyPageQuery pageQuery) {
        if(pageQuery.getAnswerId()==null&&pageQuery.getQuestionId()==null){
            throw new BadRequestException("参数错误");
        }
        Page<InteractionReply> replyPage = lambdaQuery()
                .eq(InteractionReply::getAnswerId, pageQuery.getAnswerId() == null ? 0 : pageQuery.getAnswerId())
                .eq(pageQuery.getQuestionId() != null, InteractionReply::getQuestionId, pageQuery.getQuestionId())
                .page(pageQuery.toMpPage(new OrderItem(Constant.DATA_FIELD_NAME_LIKED_TIME,false),
                        new OrderItem(Constant.DATA_FIELD_NAME_CREATE_TIME,true)));
        List<InteractionReply> replies = replyPage.getRecords();
        if(CollUtils.isEmpty(replies)){
            return PageDTO.empty(replyPage);
        }
        Set<Long> userIds = replies
                .stream()
                .filter(reply->!reply.getAnonymity())
                .map(InteractionReply::getUserId)
                .collect(Collectors.toSet());
        Set<Long> targetUserIds=replies
                .stream()
                .filter(reply->reply.getTargetUserId()!=null&&reply.getTargetUserId()>0)
                .map(InteractionReply::getTargetUserId)
                .collect(Collectors.toSet());
        Set<Long> targetReplyIds=replies
                .stream()
                .filter(reply->reply.getTargetReplyId()!=null&&reply.getTargetReplyId()>0)
                .map(InteractionReply::getTargetReplyId)
                .collect(Collectors.toSet());
        //获取用户信息
        Map<Long, UserDTO> uInfoMap = userClient.queryUserByIds(userIds)
                .stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        Map<Long, UserDTO> tuInfoMap = userClient.queryUserByIds(targetUserIds)
                .stream().collect((Collectors.toMap(UserDTO::getId, c -> c)));
        //获取目标回答信息
        Map<Long, InteractionReply> tReplyInfoMap=new HashMap<>();
        if(CollUtils.isNotEmpty(targetReplyIds)){
            tReplyInfoMap = listByIds(targetReplyIds)
                    .stream().collect(Collectors.toMap(InteractionReply::getId, c -> c));
        }
        //封装结果
        List<ReplyVO> voList=new ArrayList<>();
        for (InteractionReply reply : replies) {
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
            UserDTO userDTO = uInfoMap.get(reply.getUserId());
            if(userDTO!=null&&!vo.getAnonymity()){
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
                vo.setUserType(userDTO.getType());
            }
            UserDTO tuserInfo = tuInfoMap.get(reply.getTargetUserId());
            InteractionReply tReply = tReplyInfoMap.get(reply.getTargetReplyId());
            if(tuserInfo!=null&&tReply!=null&&!tReply.getAnonymity()){
                vo.setTargetUserName(tuserInfo.getName());
            }
            voList.add(vo);
        }
        return PageDTO.of(replyPage,voList);
    }
}
