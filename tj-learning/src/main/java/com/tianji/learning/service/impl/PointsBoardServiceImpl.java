package com.tianji.learning.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.pojo.PointsBoard;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author yizayu
 * @since 2024-10-16
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;
    /**
     * 分页查询指定赛季的积分排行榜
     *
     * @param query
     * @return
     */
    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        Long season = query.getSeason();
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARDS_SUFFIX_FORMATTER);
        boolean isCurrent= season == null || season ==0; // 如果season为空或者为0，则查询当前赛季
        PointsBoard myPointsBoard = isCurrent?queryMyCurrentBoard(key):queryMyHistoryBoard(query);
        List<PointsBoard> pointsBoardList = isCurrent ?
                queryCurrentBoardList(key, query.getPageNo(), query.getPageSize()) :
                queryHistoryBoardList(query);

        PointsBoardVO vo=new PointsBoardVO();
        if(myPointsBoard!=null){
            vo.setPoints(myPointsBoard.getPoints());
            vo.setRank(myPointsBoard.getRank());
        }
        if(CollUtils.isEmpty(pointsBoardList)){
            return vo;
        }
        //获取用户信息
        Set<Long> uIds = pointsBoardList.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOS = userClient.queryUserByIds(uIds);
        Map<Long, UserDTO> cInfoMap = new HashMap<>(userDTOS.size());
        if(CollUtils.isNotEmpty(userDTOS)){
            cInfoMap=userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }
        //封装数据
        List<PointsBoardItemVO> pointsBoardItemVOS=new ArrayList<>(pointsBoardList.size());
        for (PointsBoard pointsBoard : pointsBoardList) {
            PointsBoardItemVO itemVO=new PointsBoardItemVO();
            itemVO.setPoints(pointsBoard.getPoints());
            itemVO.setRank(pointsBoard.getRank());
            UserDTO userDTO = cInfoMap.get(pointsBoard.getUserId());
            if(userDTO!=null){
                itemVO.setName(userDTO.getName());
            }
            pointsBoardItemVOS.add(itemVO);
        }
        vo.setBoardList(pointsBoardItemVOS);
        return vo;
    }

    /**
     * 创建上赛季排行榜
     *
     * @param season
     */
    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable(LearningConstants.POINTS_BOARD_TABLE_PREFIX + season);
    }

    /**
     * 查询历史赛季积分榜
     * @param query
     * @return
     */
    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        // TODO: 2024/10/17  查询历史赛季积分榜
        return null;
    }

    /**
     * 查询当前赛季积分榜
     * @param key
     * @param pageNo
     * @param pageSize
     * @return
     */
    public List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        int form=(pageNo-1)*pageSize;
        //获取指定区间的排行人员
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, form, form + pageSize - 1);
        if (CollUtils.isEmpty(typedTuples)) {
            return CollUtils.emptyList();
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
        return pointsBoardList;
    }

    /**
     * 查询我的历史赛季积分排行
     * @param query
     * @return
     */
    private PointsBoard queryMyHistoryBoard(PointsBoardQuery query) {
        // TODO: 2024/10/17 查询我的历史赛季排行
        return null;
    }

    /**
     * 查询我的当前赛季积分排行
     * @param key
     * @return
     */
    private PointsBoard queryMyCurrentBoard(String key) {
        Long uerId = UserContext.getUser();
        Long rank = redisTemplate.opsForZSet().reverseRank(key, uerId.toString());
        Double score = redisTemplate.opsForZSet().score(key, uerId.toString());
        PointsBoard pointsBoard=new PointsBoard();
        pointsBoard.setRank(pointsBoard==null?0:rank.intValue()+1);
        pointsBoard.setPoints(pointsBoard==null?0:score.intValue());

        return pointsBoard;
    }
}
