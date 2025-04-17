package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 签到功能接口
     *
     * @return
     */
    @Override
    public SignResultVO addSignRecords() {
        //1获取用户信息
        Long userId = UserContext.getUser();
        //2拼接key
        LocalDate now = LocalDate.now();
        String key= RedisConstants.SIGN_RECORD_KEY_PREFIX+userId+now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        //3获取第几天
        int day = now.getDayOfMonth();
        int offset=day-1;
        //4签到
        Boolean setBit = redisTemplate.opsForValue().setBit(key, offset, true);
        if (setBit) {
            throw new BizIllegalException("不能重复签到");
        }
        //5获取连续签到天数
        int signDays=getSignDays(key, day);
        //计算签到得分
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        //4.保存积分明细记录
        log.info("发送签到消息========>>>>>>>>{}", SignInMessage.of(userId, rewardPoints));
        rabbitMqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1));
        // 5.封装返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    /**
     * 查询用户本月签到记录
     *
     * @return
     */
    @Override
    public List<Byte> selectMonthSignRecords() {
        //1获取用户信息
        Long userId = UserContext.getUser();
        //2拼接key
        LocalDate now = LocalDate.now();
        String key= RedisConstants.SIGN_RECORD_KEY_PREFIX+userId+now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        //3获取第几天
        int day = now.getDayOfMonth();
        List<Long> days = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if (CollUtils.isEmpty(days)){   // 判空
            throw new BizIllegalException("查询异常");
        }
        // 返回结果
        List<Byte> res = new ArrayList<>();
        Long num = days.get(0);
        while (num >= 0) {
            if (res.size() == day){
                break;
            }
            res.add(0, (byte) (num & 1));   // 尾插法解决顺序问题
            num = num>>>1;  // num为无符号整数，所以用无符号右移
        }
        return res;
    }

    private int getSignDays(String key, int day) {
        List<Long> days = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(day)).valueAt(0));
        if(CollUtils.isEmpty(days)){
            return 0;
        }
        int num = days.get(0).intValue();
        // 定义一个计数器
        int count = 0;
        // 循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) {
            // 计数器+1
            count++;
            // 把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            num >>>= 1;
        }
        return count;
    }
}