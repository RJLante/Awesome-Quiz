package com.rd.backend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rd.backend.annotation.AuthCheck;
import com.rd.backend.common.BaseResponse;
import com.rd.backend.common.DeleteRequest;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.common.ResultUtils;
import com.rd.backend.constant.UserConstant;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.exception.ThrowUtils;
import com.rd.backend.manager.AiManager;
import com.rd.backend.manager.RedisLimiterManager;
import com.rd.backend.model.dto.question.*;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.entity.Question;
import com.rd.backend.model.entity.User;
import com.rd.backend.model.enums.AppTypeEnum;
import com.rd.backend.model.vo.QuestionVO;
import com.rd.backend.model.vo.TaskIdVO;
import com.rd.backend.mq.QuestionMessageProducer;
import com.rd.backend.service.AppService;
import com.rd.backend.service.QuestionService;
import com.rd.backend.service.QuestionTaskService;
import com.rd.backend.service.UserService;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 题目接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private AiManager aiManager;

    @Resource
    private Scheduler vipScheduler;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private QuestionTaskService taskService;

    @Resource
    private QuestionMessageProducer questionMessageProducer;

    // region 增删改查

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionAddRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));

        // 数据校验
        questionService.validQuestion(question, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser();
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionUpdateRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    @PostMapping("/list/ids")
    public BaseResponse<List<QuestionVO>> listByIds(@RequestBody List<Long> ids) {
        ThrowUtils.throwIf(CollectionUtils.isEmpty(ids), ErrorCode.PARAMS_ERROR, "ids 不能为空");

        List<QuestionVO> voList = questionService.listByIds(ids).stream()
                .map(QuestionVO::objToVo)      // ← 复用你的工具方法
                .collect(Collectors.toList());

        return ResultUtils.success(voList);
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser();
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionEditRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser();
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion

    // region AI 生成功能
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
            "title \u200F是题目，options \u200B是选项，每个选项的 ke\u200Fy 按照英文字母序（比如\u200B A、B、C、D）以此类\u061C推，value 是选项内容\n" +
            "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
            "4. 返回的题目列表格式必须为 JSON 数组\n";
    /**
     * 根据应用类型生成系统提示词
     */
    private String getGenerateQuestionSystemMessage(int appType) {
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


    @PostMapping("/ai_generate/async/mq")
    public BaseResponse<TaskIdVO> aiGenerateQuestionAsyncMq(@RequestBody AiGenerateQuestionRequest req) {

        /* ---------- 1. 参数校验 ---------- */
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Long appId          = req.getAppId();
        int questionNumber  = req.getQuestionNumber();
        int optionNumber    = req.getOptionNumber();
        ThrowUtils.throwIf(questionNumber <= 0 || optionNumber <= 0,
                ErrorCode.PARAMS_ERROR, "题目数或选项数非法");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        User loginUser = userService.getLoginUser();

        /* ---------- 2. 限流 ---------- */
        // 一人一把限流器（示例 QPS = 5）
        redisLimiterManager.doRateLimit("genQuestionByAI_" + loginUser.getId());

        /* ---------- 3. 写任务表，初始 status=waiting ---------- */
        Long taskId = taskService.createTask(
                loginUser.getId(), appId, questionNumber, optionNumber);

        /* ---------- 4. 推消息到 MQ ---------- */
        questionMessageProducer.sendMessage(String.valueOf(taskId));

        /* ---------- 5. 秒回前端 ---------- */
        TaskIdVO vo = new TaskIdVO();
        vo.setTaskId(taskId);
        return ResultUtils.success(vo);
    }


    @PostMapping("/ai_generate")
    public BaseResponse<List<QuestionContentDTO>> aiGenerateQuestion(@RequestBody AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
//        // 封装 Prompt
//        String userMessage = aiManager.getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
//        // AI 生成
////        String result = aiManager.doSyncStableRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage);
//        String systemMessage = getGenerateQuestionSystemMessage(app.getAppType());
//        String result = aiManager.doSyncStableRequest(systemMessage, userMessage);
//
//        // 1. 将AI返回的完整字符串解析为JSON对象
//        JSONObject resultObj = JSONUtil.parseObj(result);
//
//        // 2. 安全地提取"message.content"字段的字符串
//        // 使用 getByPath 可以优雅地处理嵌套路径，避免空指针
//        String content = resultObj.getByPath("message.content", String.class);
//
//        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.SYSTEM_ERROR, "AI生成内容为空");
//
//        // 3. 清理content字符串，移除Markdown代码块标记和首尾空白
//        int start = content.indexOf("[");
//        int end = content.lastIndexOf("]");
//        if (start == -1 || end == -1) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成内容格式错误，未找到有效的JSON数组");
//        }
//        String json = content.substring(start, end + 1);
//        // 4. 将清理后的字符串解析为List
//        List<QuestionContentDTO> questionContentDTOList = JSONUtil.toList(json, QuestionContentDTO.class);
        List<QuestionContentDTO> questionContentDTOList =
                aiManager.generateQuestionList(app, questionNumber, optionNumber);
        return ResultUtils.success(questionContentDTOList);
    }


    @GetMapping("/ai_generate/sse")
    public SseEmitter aiGenerateQuestionSSE(AiGenerateQuestionRequest aiGenerateQuestionRequest) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = aiManager.getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // 建立 SSE 连接对象，0 表示永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        // AI 生成，SSE 流式返回
//        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        String systemMessage = getGenerateQuestionSystemMessage(app.getAppType());
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(systemMessage, userMessage, null);
        // 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取
        AtomicInteger counter = new AtomicInteger(0);
        // 拼接完整题目
        StringBuilder messageBuilder = new StringBuilder();

        // 获取登录用户
        User loginUser = userService.getLoginUser();
        // 默认全局线程池
        Scheduler scheduler = Schedulers.io();
        if (loginUser.getUserRole().equals("vip") || loginUser.getUserRole().equals("admin")) {
            scheduler = vipScheduler;
        }

        modelDataFlowable
                .observeOn(scheduler)
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    // 如果字符是左括号
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    if (counter.get() > 0) {
                        messageBuilder.append(c);
                    }
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // 可以拼接题目，并且通过 SSE 返回给前端
//                            sseEmitter.send(JSONUtil.toJsonStr(messageBuilder.toString()));
                            // 重置，准备拼接下一道题
                            String questionJson = messageBuilder.toString();
                            QuestionContentDTO dto = JSONUtil.toBean(questionJson, QuestionContentDTO.class);
                            List<QuestionContentDTO> list = new ArrayList<>();
                            list.add(dto);
                            aiManager.ensureScoreQuestionHasCorrectAnswer(app, list);
                            sseEmitter.send(JSONUtil.toJsonStr(list.get(0)));
                            messageBuilder.setLength(0);
                        }
                    }
                })
                .doOnError((e) -> log.error("Error occurred while generating questions: {}", e))
                .doOnComplete(sseEmitter::complete)
                .subscribe();

        return sseEmitter;
    }

    // endregion

    // 仅测试隔离线程池使用
    @Deprecated
    @GetMapping("/ai_generate/sse/test")
    public SseEmitter aiGenerateQuestionSSETest(AiGenerateQuestionRequest aiGenerateQuestionRequest, boolean isVip) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = aiManager.getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // 建立 SSE 连接对象，0 表示永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        // AI 生成，SSE 流式返回
//        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        String systemMessage = getGenerateQuestionSystemMessage(app.getAppType());
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(systemMessage, userMessage, null);
        // 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取
        AtomicInteger counter = new AtomicInteger(0);
        // 拼接完整题目
        StringBuilder messageBuilder = new StringBuilder();
        // 默认全局线程池
        Scheduler scheduler = Schedulers.single();
        if (isVip) {
            scheduler = vipScheduler;
        }

        modelDataFlowable
                .observeOn(scheduler)
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    // 如果字符是左括号
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    if (counter.get() > 0) {
                        messageBuilder.append(c);
                    }
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // 输出当前线程的名称
                            System.out.println("Current thread name: " + Thread.currentThread().getName());
                            // 模拟普通用户阻塞
                            if (!isVip) {
                                Thread.sleep(10000L);
                            }

                            // 可以拼接题目，并且通过 SSE 返回给前端
                            sseEmitter.send(JSONUtil.toJsonStr(messageBuilder.toString()));
                            // 重置，准备拼接下一道题
                            messageBuilder.setLength(0);
                        }
                    }
                })
                .doOnError((e) -> log.error("Error occurred while generating questions: {}", e))
                .doOnComplete(sseEmitter::complete)
                .subscribe();

        return sseEmitter;
    }
}
