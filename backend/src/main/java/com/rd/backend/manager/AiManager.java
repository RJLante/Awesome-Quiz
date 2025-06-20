package com.rd.backend.manager;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.enums.AppTypeEnum;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.springframework.stereotype.Component;

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
     * 同步请求（稳定）
     * @param systemMessage
     * @param userMessage
     * @return
     */
    public String doSyncStableRequest(String systemMessage, String userMessage)  {
        return doRequest(systemMessage, userMessage, Boolean.FALSE, STABLE_TEMPERATURE);
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
