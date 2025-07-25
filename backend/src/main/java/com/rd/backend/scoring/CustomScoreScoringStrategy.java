package com.rd.backend.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rd.backend.model.dto.question.QuestionContentDTO;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.entity.Question;
import com.rd.backend.model.entity.ScoringResult;
import com.rd.backend.model.entity.UserAnswer;
import com.rd.backend.model.vo.QuestionVO;
import com.rd.backend.service.QuestionService;
import com.rd.backend.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 自定义得分类应用评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {
    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1. 根据 id 查询到题目和题目结果信息（按分数降序排序）
        Long appId = app.getId();
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );

        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                       .orderByDesc(ScoringResult::getResultScoreRange)
        );

        // 2. 统计用户的总得分
        int totalScore = 0;

        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

        for (QuestionContentDTO questionContentDTO : questionContent) {
            for (String answer : choices) {
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    if (option.getKey().equals(answer)) {
                        String result = option.getResult();

                        int score = Optional.ofNullable(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }

        // 3. 遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果
        ScoringResult maxScoringResult = scoringResultList.get(0);

        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoringResult = scoringResult;
                break;
            }
        }

        // 4. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();

        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}
