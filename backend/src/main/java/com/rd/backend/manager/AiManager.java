package com.rd.backend.manager;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import cn.hutool.json.JSONUtil;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.model.dto.question.QuestionContentDTO;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.enums.AppTypeEnum;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * ai请求管理器
 */
@Component
public class AiManager {

    @Resource
    private ClientV4 clientV4;
    private static final float STABLE_TEMPERATURE = 0.05f;
    private static final float UNSTABLE_TEMPERATURE = 0.99f;

    private static final int DEFAULT_BATCH_SIZE = 10;

    private static final String GENERATE_QUESTION_SYSTEM_MESSAGE = "你是一位严谨的出题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "应用类别，\n" +
            "要生成的题目数，\n" +
            "每个题目的选项数\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来出题：\n" +
            "1. 要求：题目和选项尽可能地短，题目不要包含序号，每题的选项数以我提供的为主，题目不能重复\n" +
            "2. 严格按照下面的 json 格式输出题目和选项\n" +
            "```\n" +
            "[{\"options\":[{\"value\":\"选项内容\",\"key\":\"A\"},{\"value\":\"\",\"key\":\"B\"}],\"title\":\"题目标题\"}]\n" +
            "```\n" +
            "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容\n" +
            "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
            "4. 返回的题目列表格式必须为 JSON 数组\n";

    /**
     * 根据应用类型生成系统提示词
     */
    public String getGenerateQuestionSystemMessage(int appType) {
        String base = "你是一位严谨的出题专家，我会给你如下信息：\n" +
                "```\n" +
                "应用名称，\n" +
                "【【【应用描述】】】，\n" +
                "应用类别，\n" +
                "要生成的题目数，\n" +
                "每个题目的选项数\n" +
                "```\n" +
                "\n" +
                "请你根据上述信息，按照以下步骤来出题：\n" +
                "1. 要求：题目和选项尽可能地短，题目不要包含序号，每题的选项数以我提供的为主，题目不能重复\n";

        String format;
        if (appType == AppTypeEnum.SCORE.getValue()) {
            format = "[{\\\"options\\\":[{\\\"value\\\":\\\"选项内容\\\",\\\"score\\\":0,\\\"key\\\":\\\"A\\\"}],\\\"title\\\":\\\"题目标题\\\"}]";
            base += "2. 严格按照下面的 json 格式输出题目和选项\n" +
                    "```\n" +
                    format + "\n" +
                    "```\n" +
                    "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容\n" +
                    "3. 每道题必须只有一个正确答案，正确答案的score为1，其余为0，正确答案的选项位置要随机\n" +
                    "4. 检查题目是否包含序号，若包含序号则去除序号\n" +
                    "5. 返回的题目列表格式必须为 JSON 数组\n";
            return base;
        } else {
            format = "[{\\\"options\\\":[{\\\"value\\\":\\\"选项内容\\\",\\\"result\\\":\\\"属性\\\",\\\"key\\\":\\\"A\\\"}],\\\"title\\\":\\\"题目标题\\\"}]";
        }

        base += "2. 严格按照下面的 json 格式输出题目和选项\n" +
                "```\n" +
                format + "\n" +
                "```\n" +
                "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容\n" +
                "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
                "4. 返回的题目列表格式必须为 JSON 数组\n";
        return base;
    }


    public List<String> generateQuestionsInBatches(String systemMessage, App app, int totalNumber, int optionNumber) {
        // 存储所有批次AI生成的JSON结果
        List<String> allJsonResults = new ArrayList<>();
        // 维护完整的对话历史，用于去重
        List<ChatMessage> conversationHistory = new ArrayList<>();
        // 1. 添加初始的系统消息
        conversationHistory.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage));

        int remainingNumber = totalNumber;
        while (remainingNumber > 0) {
            // 2. 计算当前批次需要生成的数量
            int currentBatchSize = Math.min(remainingNumber, DEFAULT_BATCH_SIZE);

            // 3. 【关键】为当前批次动态构建用户消息
            String currentUserMessage = buildUserMessageForBatch(app, currentBatchSize, optionNumber);

            // 4. 为本次调用创建请求消息列表
            //    包含完整的历史对话 + 本次新的用户请求
            List<ChatMessage> requestMessages = new ArrayList<>(conversationHistory);
            requestMessages.add(new ChatMessage(ChatMessageRole.USER.value(), currentUserMessage));

            // 5. 调用AI
            String aiResponseContent = doRequest(requestMessages, Boolean.FALSE, STABLE_TEMPERATURE);

            // 6. 清理和提取AI返回的JSON内容
            String cleanJson = extractJsonArray(aiResponseContent);
            if (StrUtil.isBlank(cleanJson)) {
                System.err.println("Warning: AI for a batch returned empty or invalid content. Skipping.");
                remainingNumber -= currentBatchSize; // 即使失败也继续，防止死循环
                continue;
            }

            // 7. 保存本次结果
            allJsonResults.add(cleanJson);

            // 8. 【核心】将AI的本次回答加入到对话历史中，作为下一次请求的上下文
            conversationHistory.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), cleanJson));

            // 9. 更新剩余数量
            remainingNumber -= currentBatchSize;
        }

        return allJsonResults;
    }

    /**
     * 【新的私有底层方法】 - 负责实际的API调用
     * 这个方法只负责调用SDK并返回原始的 ModelApiResponse 对象。
     */
    private ModelApiResponse doRequestAndGetResponse(List<ChatMessage> messages, float temperature) {
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .temperature(temperature)
                .messages(messages)
                .build();
        try {
            // 注意：这里需要设置 invokeMethod，否则 ClientV4 源码会返回 null
            chatCompletionRequest.setInvokeMethod(Constants.invokeMethod); // "invoke" for sync
            return clientV4.invokeModelApi(chatCompletionRequest);
        } catch (Exception e) {
            // 捕获SDK可能抛出的其他网络或运行时异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用AI接口时发生异常: " + e.getMessage());
        }
    }

    private String buildUserMessageForBatch(App app, int questionNumber, int optionNumber) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        userMessage.append(AppTypeEnum.getEnumByValue(app.getAppType()).getText()).append("类").append("\n");
        userMessage.append(questionNumber).append("\n"); // 使用批次的数量
        userMessage.append(optionNumber);
        return userMessage.toString();
    }

    /**
     * 【新增辅助方法】从AI的回复中提取干净的JSON数组字符串
     * @param content AI返回的原始content
     * @return 干净的JSON数组字符串，如 "[]" 或 "[{...}]"
     */
    private String extractJsonArray(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start == -1 || end == -1 || start > end) {
            // 也可以在这里抛出异常，如果格式错误是不可接受的
            // throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成内容格式错误，未找到有效的JSON数组");
            return null;
        }
        return content.substring(start, end + 1);
    }

    /**
     * 通用请求 (底层)
     * @param messages 消息列表
     * @param stream 是否流式
     * @param temperature 温度
     * @return ModelApiResponse 完整的API响应
     */
    public ModelApiResponse doRequestModel(List<ChatMessage> messages, Boolean stream, Float temperature) {
        // 构造请求
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(stream)
                .temperature(temperature)
                .messages(messages)
                .build();
        try {
            return clientV4.invokeModelApi(chatCompletionRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用AI接口失败: " + e.getMessage());
        }
    }


    /**
     * 生成题目的用户消息
     * @param app
     * @param questionNumber
     * @param optionNumber
     * @return
     */
    public String getGenerateQuestionUserMessage(App app, int questionNumber, int optionNumber) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        userMessage.append(AppTypeEnum.getEnumByValue(app.getAppType()).getText() + "类").append("\n");
        userMessage.append(questionNumber).append("\n");
        userMessage.append(optionNumber);
        return userMessage.toString();
    }

    /**
     * 统一生成题目并解析为 DTO 列表
     *
     * @param app            应用元信息
     * @param questionNumber 题目数
     * @param optionNumber   选项数
     * @return 解析后的题目列表
     */
    public List<QuestionContentDTO> generateQuestionList(App app,
                                                         int questionNumber,
                                                         int optionNumber) {
        // 1. 构造用户消息
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // 2. 根据应用类型生成系统提示词并调用 AI
        String systemMessage = getGenerateQuestionSystemMessage(app.getAppType());
        String result = doSyncStableRequest(systemMessage, userMessage);
        // 3. 解析为 QuestionContentDTO 列表
        String content = JSONUtil.parseObj(result).getByPath("message.content", String.class);
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成内容为空");
        }
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start == -1 || end == -1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成格式错误");
        }
        String json = content.substring(start, end + 1);
//        return JSONUtil.toList(json, QuestionContentDTO.class);
        List<QuestionContentDTO> list = JSONUtil.toList(json, QuestionContentDTO.class);
        return list;
    }



    /**
     * 同步请求（稳定）
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncStableRequest(String systemMessage, String userMessage)  {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, STABLE_TEMPERATURE);
    }
    /**
     * 流式请求（稳定）
     *
     * @param systemMessage 系统提示词
     * @param userMessage   用户消息
     * @return Flowable 流式响应
     */
    public Flowable<ModelData> doStreamStableRequest(String systemMessage, String userMessage) {
        return doStreamRequest(systemMessage, userMessage, STABLE_TEMPERATURE);
    }

    /**
     * 同步请求（不稳定）
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncUnstableRequest(String systemMessage, String userMessage)  {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, UNSTABLE_TEMPERATURE);
    }

    /**
     * 同步请求
     * @param systemMessage
     * @param userMessage
     * @param temperature
     * @return
     */
    public String doSyncRequest(String systemMessage, String userMessage, Float temperature)  {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, temperature);
    }

    /**
     * 通用请求 (简化消息传递)
     * @param systemMessage
     * @param userMessage
     * @param stream
     * @param temperature
     * @return
     */
    public String doRequest(String systemMessage, String userMessage, Boolean stream, Float temperature) {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        chatMessageList.add(systemChatMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        chatMessageList.add(userChatMessage);
        return doRequest(chatMessageList, stream, temperature);
    }

    /**
     * 通用请求
     * @param messages
     * @param stream
     * @param temperature
     * @return
     */
    public String doRequest(List<ChatMessage> messages, Boolean stream, Float temperature) {
        // 构造请求
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(stream)
                .temperature(temperature)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        try {
            ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
            return invokeModelApiResp.getData().getChoices().get(0).toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    /**
     * 通用流式请求 (简化消息传递)
     * @param systemMessage
     * @param userMessage
     * @param temperature
     * @return
     */
    public Flowable<ModelData> doStreamRequest(String systemMessage, String userMessage, Float temperature) {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessage systemChatMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage);
        chatMessageList.add(systemChatMessage);
        ChatMessage userChatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        chatMessageList.add(userChatMessage);

        return doStreamRequest(chatMessageList, temperature);
    }

    /**
     * 通用流式请求
     * @param messages
     * @param temperature
     * @return
     */
    public Flowable<ModelData> doStreamRequest(List<ChatMessage> messages, Float temperature) {
        // 构造请求
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .temperature(temperature)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        try {
            ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
            return invokeModelApiResp.getFlowable();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }
}
