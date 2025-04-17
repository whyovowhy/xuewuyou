package com.tianji.learning.constants;

public interface RedisConstants {
    /**
     * 签到记录前缀
     */
    String SIGN_RECORD_KEY_PREFIX="sign:uid:";
    /**
     *积分排行榜前缀   yyyyMM
     */
    String POINTS_BOARD_KEY_PREFIX="boards:";
}