package com.tianji.learning.task;

import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.pojo.PointsBoard;
import com.tianji.learning.pojo.PointsBoardSeason;
import com.tianji.learning.service.impl.PointsBoardSeasonServiceImpl;
import com.tianji.learning.service.impl.PointsBoardServiceImpl;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsBoardPersistentHandler {
    private final PointsBoardServiceImpl pointsBoardService;
    private final PointsBoardSeasonServiceImpl pointsBoardSeasonService;

    private final StringRedisTemplate redisTemplate;
    @XxlJob("createTableJob")
    public void createPointsBoardTable() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMounth = now.minusMonths(1); // 获取上个月的时间
        PointsBoardSeason season = pointsBoardSeasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, lastMounth)
                .ge(PointsBoardSeason::getEndTime, lastMounth)
                .one();
        //创建赛季排行榜
        log.info("===================创建第{}赛季排行榜===================", season.getId());
        pointsBoardService.createPointsBoardTableBySeason(season.getId());
    }
    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB() {
        // 获取上赛季数据库表名
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMounth = now.minusMonths(3);
        PointsBoardSeason season = pointsBoardSeasonService.lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, lastMounth)
                .ge(PointsBoardSeason::getEndTime, lastMounth)
                .one();
        TableInfoContext.setInfo(LearningConstants.POINTS_BOARD_TABLE_PREFIX+season.getId());
        //获取redis上赛季数据
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+lastMounth.format(DateUtils.POINTS_BOARDS_SUFFIX_FORMATTER);
        log.info("===================持久化{}赛季排行榜数据到DB===================",key);

        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();

        int pageNo=1+index;
        int pageSize=3;
        List<PointsBoard> pointsBoardList =new ArrayList<>();
        while(true){
            log.info("分片序号：{}，总分片数：{}，当前页码：{}",index,total,pageNo);
            List<PointsBoard>redisPointsBoardList = pointsBoardService.queryCurrentBoardList(key, pageNo, pageSize);
            if(CollUtils.isEmpty(redisPointsBoardList)){
                break;
            }
            redisPointsBoardList.forEach(pointsBoard -> {
                pointsBoard.setId(pointsBoard.getRank().longValue());
                pointsBoard.setRank(null);
            });
//            redisPointsBoardList = redisPointsBoardList.stream().map(pointsBoard -> {
//                pointsBoard.setId(pointsBoard.getRank().longValue());
//                pointsBoard.setRank(null);
//                return pointsBoard;
//            }).collect(Collectors.toList());
            pointsBoardList.addAll(redisPointsBoardList);
            pageNo+=total;
        }
        if(CollUtils.isEmpty(pointsBoardList)){
            return;
        }
        pointsBoardService.saveBatch(pointsBoardList);
        TableInfoContext.remove();
    }

    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.计算key
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARDS_SUFFIX_FORMATTER);
        // 3.删除
        log.info("===================清理{}赛季排行榜数据到DB===================",key);
        redisTemplate.unlink(key);
    }
}