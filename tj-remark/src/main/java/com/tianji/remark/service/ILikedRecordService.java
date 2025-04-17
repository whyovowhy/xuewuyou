package com.tianji.remark.service;

import com.tianji.remark.domian.LikeRecordFormDTO;
import com.tianji.remark.pojo.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-14
 */
public interface ILikedRecordService extends IService<LikedRecord> {
    /**
     * 点赞或取消点赞
     * @param recordDTO
     */
    void addLikeRecord(LikeRecordFormDTO recordDTO);

    /**
     * 查询指定业务id的点赞状态
     * @param bizIds
     * @return
     */
    Set<Long> isBizLiked(List<Long> bizIds);
    /**
     * 从redis取指定类型点赞数量并发送消息到RabbitMQ
     *
     * @param bizType    业务类型
     * @param maxBizSize 每次任务取出的业务score标准
     */
    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);
}
