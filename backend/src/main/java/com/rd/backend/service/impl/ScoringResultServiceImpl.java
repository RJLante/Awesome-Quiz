package com.rd.backend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.constant.CommonConstant;
import com.rd.backend.exception.ThrowUtils;
import com.rd.backend.mapper.ScoringResultMapper;
import com.rd.backend.model.dto.scoringResult.ScoringResultQueryRequest;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.entity.ScoringResult;
//import com.rd.backend.model.entity.ScoringResultFavour;
//import com.rd.backend.model.entity.ScoringResultThumb;
import com.rd.backend.model.entity.User;
import com.rd.backend.model.vo.ScoringResultVO;
import com.rd.backend.model.vo.UserVO;
import com.rd.backend.service.AppService;
import com.rd.backend.service.ScoringResultService;
import com.rd.backend.service.UserService;
import com.rd.backend.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评分结果服务实现
 *
 */
@Service
@Slf4j
public class ScoringResultServiceImpl extends ServiceImpl<ScoringResultMapper, ScoringResult> implements ScoringResultService {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    /**
     * 校验数据
     *
     * @param scoringResult
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validScoringResult(ScoringResult scoringResult, boolean add) {
        ThrowUtils.throwIf(scoringResult == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String resultName = scoringResult.getResultName();
        Integer resultScoreRange = scoringResult.getResultScoreRange();
        Long appId = scoringResult.getAppId();

        // 创建数据时，参数不能为空
        if (add) {
            // 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(resultName), ErrorCode.PARAMS_ERROR, "结果名称不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 不合法");
        }
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (StringUtils.isNotBlank(resultName)) {
            ThrowUtils.throwIf(resultName.length() > 128, ErrorCode.PARAMS_ERROR, "结果名称不能超过128个字符");
        }
        if (resultScoreRange != null) {
            ThrowUtils.throwIf(resultScoreRange < 0, ErrorCode.PARAMS_ERROR, "分数范围不能小于0");
        }
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param scoringResultQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<ScoringResult> getQueryWrapper(ScoringResultQueryRequest scoringResultQueryRequest) {
        QueryWrapper<ScoringResult> queryWrapper = new QueryWrapper<>();
        if (scoringResultQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = scoringResultQueryRequest.getId();
        String resultName = scoringResultQueryRequest.getResultName();
        String resultDesc = scoringResultQueryRequest.getResultDesc();
        String resultPicture = scoringResultQueryRequest.getResultPicture();
        String resultProp = scoringResultQueryRequest.getResultProp();
        Integer resultScoreRange = scoringResultQueryRequest.getResultScoreRange();
        Long appId = scoringResultQueryRequest.getAppId();
        Long userId = scoringResultQueryRequest.getUserId();
        Long notId = scoringResultQueryRequest.getNotId();
        String searchText = scoringResultQueryRequest.getSearchText();
        String sortField = scoringResultQueryRequest.getSortField();
        String sortOrder = scoringResultQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("resultName", searchText).or().like("resultDesc", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(resultName), "resultName", resultName);
        queryWrapper.like(StringUtils.isNotBlank(resultDesc), "resultDesc", resultDesc);
        queryWrapper.like(StringUtils.isNotBlank(resultProp), "resultProp", resultProp);

        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultPicture), "resultPicture", resultPicture);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultScoreRange), "resultScoreRange", resultScoreRange);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取评分结果封装
     *
     * @param scoringResult
     * @param request
     * @return
     */
    @Override
    public ScoringResultVO getScoringResultVO(ScoringResult scoringResult, HttpServletRequest request) {
        // 对象转封装类
        ScoringResultVO scoringResultVO = ScoringResultVO.objToVo(scoringResult);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = scoringResult.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        scoringResultVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
//        long scoringResultId = scoringResult.getId();
//        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            // 获取点赞
//            QueryWrapper<ScoringResultThumb> scoringResultThumbQueryWrapper = new QueryWrapper<>();
//            scoringResultThumbQueryWrapper.in("scoringResultId", scoringResultId);
//            scoringResultThumbQueryWrapper.eq("userId", loginUser.getId());
//            ScoringResultThumb scoringResultThumb = scoringResultThumbMapper.selectOne(scoringResultThumbQueryWrapper);
//            scoringResultVO.setHasThumb(scoringResultThumb != null);
//            // 获取收藏
//            QueryWrapper<ScoringResultFavour> scoringResultFavourQueryWrapper = new QueryWrapper<>();
//            scoringResultFavourQueryWrapper.in("scoringResultId", scoringResultId);
//            scoringResultFavourQueryWrapper.eq("userId", loginUser.getId());
//            ScoringResultFavour scoringResultFavour = scoringResultFavourMapper.selectOne(scoringResultFavourQueryWrapper);
//            scoringResultVO.setHasFavour(scoringResultFavour != null);
//        }
        // endregion

        return scoringResultVO;
    }

    /**
     * 分页获取评分结果封装
     *
     * @param scoringResultPage
     * @param request
     * @return
     */
    @Override
    public Page<ScoringResultVO> getScoringResultVOPage(Page<ScoringResult> scoringResultPage, HttpServletRequest request) {
        List<ScoringResult> scoringResultList = scoringResultPage.getRecords();
        Page<ScoringResultVO> scoringResultVOPage = new Page<>(scoringResultPage.getCurrent(), scoringResultPage.getSize(), scoringResultPage.getTotal());
        if (CollUtil.isEmpty(scoringResultList)) {
            return scoringResultVOPage;
        }
        // 对象列表 => 封装对象列表
        List<ScoringResultVO> scoringResultVOList = scoringResultList.stream().map(scoringResult -> {
            return ScoringResultVO.objToVo(scoringResult);
        }).collect(Collectors.toList());

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = scoringResultList.stream().map(ScoringResult::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
//        Map<Long, Boolean> scoringResultIdHasThumbMap = new HashMap<>();
//        Map<Long, Boolean> scoringResultIdHasFavourMap = new HashMap<>();
//        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            Set<Long> scoringResultIdSet = scoringResultList.stream().map(ScoringResult::getId).collect(Collectors.toSet());
//            loginUser = userService.getLoginUser(request);
//            // 获取点赞
//            QueryWrapper<ScoringResultThumb> scoringResultThumbQueryWrapper = new QueryWrapper<>();
//            scoringResultThumbQueryWrapper.in("scoringResultId", scoringResultIdSet);
//            scoringResultThumbQueryWrapper.eq("userId", loginUser.getId());
//            List<ScoringResultThumb> scoringResultScoringResultThumbList = scoringResultThumbMapper.selectList(scoringResultThumbQueryWrapper);
//            scoringResultScoringResultThumbList.forEach(scoringResultScoringResultThumb -> scoringResultIdHasThumbMap.put(scoringResultScoringResultThumb.getScoringResultId(), true));
//            // 获取收藏
//            QueryWrapper<ScoringResultFavour> scoringResultFavourQueryWrapper = new QueryWrapper<>();
//            scoringResultFavourQueryWrapper.in("scoringResultId", scoringResultIdSet);
//            scoringResultFavourQueryWrapper.eq("userId", loginUser.getId());
//            List<ScoringResultFavour> scoringResultFavourList = scoringResultFavourMapper.selectList(scoringResultFavourQueryWrapper);
//            scoringResultFavourList.forEach(scoringResultFavour -> scoringResultIdHasFavourMap.put(scoringResultFavour.getScoringResultId(), true));
//        }
        // 填充信息
        scoringResultVOList.forEach(scoringResultVO -> {
            Long userId = scoringResultVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            scoringResultVO.setUser(userService.getUserVO(user));
//            scoringResultVO.setHasThumb(scoringResultIdHasThumbMap.getOrDefault(scoringResultVO.getId(), false));
//            scoringResultVO.setHasFavour(scoringResultIdHasFavourMap.getOrDefault(scoringResultVO.getId(), false));
        });
        // endregion

        scoringResultVOPage.setRecords(scoringResultVOList);
        return scoringResultVOPage;
    }

}
