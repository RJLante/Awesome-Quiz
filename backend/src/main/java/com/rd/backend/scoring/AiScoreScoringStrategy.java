package com.rd.backend.scoring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.exception.ThrowUtils;
import com.rd.backend.manager.AiManager;
import com.rd.backend.model.dto.question.QuestionAnswerDTO;
import com.rd.backend.model.dto.question.QuestionContentDTO;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.entity.Question;
import com.rd.backend.model.entity.UserAnswer;
import com.rd.backend.model.vo.QuestionVO;
import com.rd.backend.service.QuestionService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 得分类应用评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 1)
public class AiScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedissonClient redissonClient;

    private static final String AI_SCORE_LOCK = "AI_SCORE_LOCK";

    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    .expireAfterAccess(5L, TimeUnit.MINUTES)
                    .build();

    private static final String AI_SCORE_SCORING_SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\\\"title\\\": \\\"题目\\\",\\\"answer\\\": \\\"用户回答\\\"}]\n" +
            "用户得分\n" +
            "```\n" +
            "\n" +
            "请你根据用户得分和上述信息，给出一个明确的评价结果，包括评价名称（尽量简短）和评价描述（尽量详细，大于 200 字）\n" +
            "严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\\\"resultName\\\": \\\"评价名称\\\", \\\"resultDesc\\\": \\\"评价描述\\\"}\n" +
            "```\n" +
            "返回格式必须为 JSON 对象";

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        String jsonStr = JSONUtil.toJsonStr(choices);
        String cacheKey = buildCacheKey(appId, jsonStr);
        String answerJson = answerCacheMap.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(answerJson)) {
            UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            return userAnswer;
        }

        RLock lock = redissonClient.getLock(AI_SCORE_LOCK + cacheKey);
        try {
            boolean res = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!res) {
                return null;
            }
            answerJson = answerCacheMap.getIfPresent(cacheKey);
            if (StrUtil.isNotBlank(answerJson)) {
                UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
                userAnswer.setAppId(appId);
                userAnswer.setAppType(app.getAppType());
                userAnswer.setScoringStrategy(app.getScoringStrategy());
                userAnswer.setChoices(jsonStr);
                return userAnswer;
            }

            Question question = questionService.getOne(
                    Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
            );
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

            int totalScore = 0;
            for (int i = 0; i < questionContent.size(); i++) {
                QuestionContentDTO questionContentDTO = questionContent.get(i);
                String choice = choices.get(i);
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()) {
                    if (option.getKey().equals(choice)) {
                        totalScore += option.getScore();
                        break;
                    }
                }
            }

            String userMessage = getAiScoreScoringUserMessage(app, questionContent, choices, totalScore);
            String result = aiManager.doSyncStableRequest(AI_SCORE_SCORING_SYSTEM_MESSAGE, userMessage);

            JSONObject resultObj = JSONUtil.parseObj(result);
            String content = resultObj.getByPath("message.content", String.class);
            ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.SYSTEM_ERROR, "AI生成内容为空");

            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (start == -1 || end == -1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成内容格式错误，未找到有效的JSON数组");
            }
            String json = content.substring(start, end + 1);

            answerCacheMap.put(cacheKey, json);

            UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            userAnswer.setResultScore(totalScore);
            return userAnswer;
        } finally {
            if (lock != null && lock.isLocked()) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private String getAiScoreScoringUserMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices, int score) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList)).append("\n");
        userMessage.append(score);
        return userMessage.toString();
    }

    private String buildCacheKey(Long appId, String choices) {
        return DigestUtil.md5Hex(appId + ":" + choices);
    }
}