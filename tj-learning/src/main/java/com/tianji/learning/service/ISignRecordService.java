package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

import java.util.List;

public interface ISignRecordService {
    /**
     * 签到功能接口
     * @return
     */
    SignResultVO addSignRecords();

    /**
     * 查询用户本月签到记录
     * @return
     */
    List<Byte> selectMonthSignRecords();
}
