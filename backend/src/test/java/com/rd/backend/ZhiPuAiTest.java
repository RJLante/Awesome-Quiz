package com.rd.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class ZhiPuAiTest {

    @Resource
    private ClientV4 clientV4;

    @Test
    public void test() {
        // 初始化客户端
//        ClientV4 client = new ClientV4.Builder(KeyConstant.API_SECRET_KEY).build();

        // 构造请求
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "你是一位严谨的出题专家，我会给你如下信息：\n" +
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
                "title \u200F是题目，options \u200B是选项，每个选项的 ke\u200Fy 按照英文字母序（比如\u200B A、B、C、D）以此类\u061C推，value 是选项内容\n" +
                "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
                "4. 返回的题目列表格式必须为 JSON 数组\n" +
                "MBTI性格测试" +
                "用于MBTI性格测试，生成测试题目，通过测试来计算用户的角色人格" +
                "测评类" +
                "10" +
                "4");
        messages.add(chatMessage);
//        String requestId = String.format("YourRequestId-d%", System.currentTimeMillis());
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        System.out.println("model output:" + invokeModelApiResp.getData().getChoices().get(0));
    }
}
