package com.tianji.learning.service.impl;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.pojo.PointsBoard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SpringBootTest

class PointsRecordServiceImplTest {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Test
    void test(){
        Long uerId = 129L;
        Long rank = redisTemplate.opsForZSet().reverseRank("boards:202407", uerId.toString());
        Double score = redisTemplate.opsForZSet().score("boards:202407", uerId.toString());
        PointsBoard pointsBoard=new PointsBoard();
        pointsBoard.setRank(rank.intValue());
        pointsBoard.setPoints(score.intValue());

        System.out.println(pointsBoard);
    }
    @Test
    void test1(){
        int form=(1-1)*10;
        //获取指定区间的排行人员
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores("boards:202407", form, form + 10 - 1);
        if (CollUtils.isEmpty(typedTuples)) {
            System.out.println("没有数据");
        }
        List<PointsBoard> pointsBoardList = new ArrayList<>(typedTuples.size());

        int rank=form+1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            PointsBoard pointsBoard=new PointsBoard();
            Double score = typedTuple.getScore();
            String value = typedTuple.getValue();
            if(score==null|| StringUtils.isBlank(value)){
                continue;
            }
            pointsBoard.setPoints(score.intValue());
            pointsBoard.setUserId(Long.valueOf(value));
            pointsBoard.setRank(rank);
            pointsBoardList.add(pointsBoard);
            rank++;

        }
        System.out.println(pointsBoardList);
    }
}