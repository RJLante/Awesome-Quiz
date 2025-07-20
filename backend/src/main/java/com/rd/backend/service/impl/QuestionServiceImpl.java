package com.rd.backend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.constant.CommonConstant;
import com.rd.backend.exception.ThrowUtils;
import com.rd.backend.mapper.QuestionMapper;
import com.rd.backend.model.dto.question.QuestionQueryRequest;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.entity.Question;
//import com.rd.backend.model.entity.QuestionFavour;
//import com.rd.backend.model.entity.QuestionThumb;
import com.rd.backend.model.entity.User;
import com.rd.backend.model.vo.QuestionVO;
import com.rd.backend.model.vo.UserVO;
import com.rd.backend.service.AppService;
import com.rd.backend.service.QuestionService;
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
 * 题目服务实现
 *
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String questionContent = question.getQuestionContent();
        Long appId = question.getAppId();
        // 创建数据时，参数不能为空
        if (add) {
            // 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(questionContent), ErrorCode.PARAMS_ERROR, "题目内容不能为空");
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 不合法");
        }
        // 修改数据时，有参数则校验
        // 补充校验规则
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = questionQueryRequest.getId();
        String questionContent = questionQueryRequest.getQuestionContent();
        Long appId = questionQueryRequest.getAppId();
        Long userId = questionQueryRequest.getUserId();
        Long notId = questionQueryRequest.getNotId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(questionContent), "questionContent", questionContent);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
//        long questionId = question.getId();
//        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            // 获取点赞
//            QueryWrapper<QuestionThumb> questionThumbQueryWrapper = new QueryWrapper<>();
//            questionThumbQueryWrapper.in("questionId", questionId);
//            questionThumbQueryWrapper.eq("userId", loginUser.getId());
//            QuestionThumb questionThumb = questionThumbMapper.selectOne(questionThumbQueryWrapper);
//            questionVO.setHasThumb(questionThumb != null);
//            // 获取收藏
//            QueryWrapper<QuestionFavour> questionFavourQueryWrapper = new QueryWrapper<>();
//            questionFavourQueryWrapper.in("questionId", questionId);
//            questionFavourQueryWrapper.eq("userId", loginUser.getId());
//            QuestionFavour questionFavour = questionFavourMapper.selectOne(questionFavourQueryWrapper);
//            questionVO.setHasFavour(questionFavour != null);
//        }
        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(),
                questionPage.getSize(), questionPage.getTotal());

        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 将 Question 分页结果转成 QuestionVO 分页结果
     */
//    @Override
//    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage,
//                                              HttpServletRequest request) {
//
//        // ---------- 0. 拿出当前记录 ----------
//        List<Question> questionList = questionPage.getRecords();
//
//        // ---------- 1. 若 pageSize = 1 但实际 total >1，则兜底重新查询 ----------
//        if (questionPage.getSize() == 1 && questionPage.getTotal() > 1) {
//
//            // 当前应用 id
//            Long appId = questionList.isEmpty()
//                    ? null
//                    : questionList.get(0).getAppId();
//
//            if (appId != null) {
//                // 一次拉全
//                questionList = this.lambdaQuery()
//                        .eq(Question::getAppId, appId)
//                        .orderByAsc(Question::getCreateTime)
//                        .list();
//
//                // 👉 同步修改分页对象的 size / total / records
//                questionPage.setRecords(questionList);
//                questionPage.setSize(questionList.size());
//                questionPage.setTotal(questionList.size());
//            }
//        }
//
//        // ---------- 2. 构造返回 Page<QuestionVO> ----------
//        Page<QuestionVO> voPage = new Page<>(
//                questionPage.getCurrent(),
//                questionPage.getSize(),
//                questionPage.getTotal());
//
//        if (CollUtil.isEmpty(questionList)) {
//            return voPage;
//        }
//
//        // ---------- 3. 转 VO ----------
//        List<QuestionVO> voList = questionList.stream()
//                .map(QuestionVO::objToVo)
//                .collect(Collectors.toList());
//
//        // ---------- 4. 填充用户 ----------
//        Set<Long> uidSet = questionList.stream()
//                .map(Question::getUserId)
//                .collect(Collectors.toSet());
//
//        Map<Long, List<User>> uidMap = userService.listByIds(uidSet).stream()
//                .collect(Collectors.groupingBy(User::getId));
//
//        voList.forEach(vo -> {
//            User u = Optional.ofNullable(uidMap.get(vo.getUserId()))
//                    .map(l -> l.get(0))
//                    .orElse(null);
//            vo.setUser(userService.getUserVO(u));
//        });
//
//        voPage.setRecords(voList);
//        return voPage;
//    }



}
