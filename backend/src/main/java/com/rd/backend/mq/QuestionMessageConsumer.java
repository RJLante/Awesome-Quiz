package com.rd.backend.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.manager.AiManager;
import com.rd.backend.model.dto.question.QuestionContentDTO;
import com.rd.backend.model.entity.App;
import com.rd.backend.model.entity.Question;
import com.rd.backend.model.entity.QuestionTask;
import com.rd.backend.service.AppService;
import com.rd.backend.service.QuestionService;
import com.rd.backend.service.QuestionTaskService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class QuestionMessageConsumer {

    @Resource
    private QuestionTaskService taskService;

    @Resource
    private AppService appService;

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    @SneakyThrows
    @RabbitListener(queues = {QuestionMqConstant.QUESTION_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);

        // 判空 + Nack
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        long taskId = Long.parseLong(message);
        QuestionTask task = taskService.getById(taskId);
        if (task == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        }

        QuestionTask update = new QuestionTask();
        update.setId(task.getId());
        update.setStatus("running");
        boolean running = taskService.updateById(update);
        if (!running) {
            channel.basicNack(deliveryTag, false, false);
            handleTaskError(task.getId(), "更新任务执行中状态失败");
            return;
        }

        try {
            // 0. 验证 & 加载应用
            App app = appService.getById(task.getAppId());
            if (app == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
            }

            // 1. 进度 30%：开始调用 AI
            taskService.updateProgress(taskId, 30);

            // AI 生成 N 道题，返回 List<QuestionContentDTO>
            List<QuestionContentDTO> dtoList = aiManager.generateQuestionList(
                    app, task.getQuestionNum(), task.getOptionNum());

            // 2. 进度 70%：AI 调用完成
            taskService.updateProgress(taskId, 70);

            // 3. **仅保存 1 条记录**：把整套 dtoList 序列化为一个 JSON 数组字符串
            Question exist = questionService.getOne(
                    Wrappers.<Question>lambdaQuery().eq(Question::getAppId, app.getId()), false);
            if (exist != null) {
                List<QuestionContentDTO> existingList =
                               JSONUtil.toList(exist.getQuestionContent(), QuestionContentDTO.class);
                  if (existingList == null) {
                      existingList = new ArrayList<>();
                  }
                existingList.addAll(dtoList);
                exist.setQuestionContent(JSONUtil.toJsonStr(existingList));
                questionService.updateById(exist);
                // 把新生成的题目 id 传给 taskService.finish
                taskService.updateProgress(taskId, 90);
                taskService.finish(taskId, Collections.singletonList(exist.getId()));
            } else {
                Question q = new Question();
                q.setAppId(app.getId());
                q.setUserId(task.getUserId());
                q.setQuestionContent(JSONUtil.toJsonStr(dtoList));
                questionService.save(q);
                taskService.updateProgress(taskId, 90);
                taskService.finish(taskId, Collections.singletonList(q.getId()));
            }
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            handleTaskError(taskId, e.getMessage());
        }
    }


    private void handleTaskError(long taskId, String execMessage) {
        QuestionTask fail = new QuestionTask();
        fail.setId(taskId);
        fail.setStatus("failed");
        fail.setExecMessage(execMessage);
        boolean ok = taskService.updateById(fail);
        if (!ok) {
            log.error("更新任务失败状态失败" + taskId + "," + execMessage);
        }
    }

}
