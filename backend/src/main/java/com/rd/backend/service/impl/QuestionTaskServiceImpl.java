package com.rd.backend.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rd.backend.mapper.QuestionTaskMapper;
import com.rd.backend.model.entity.QuestionTask;
import com.rd.backend.service.QuestionTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class QuestionTaskServiceImpl
        extends ServiceImpl<QuestionTaskMapper, QuestionTask>
        implements QuestionTaskService {

    @Override
    @Transactional
    public Long createTask(Long userId, Long appId, int questionNum, int optionNum) {
        QuestionTask task = new QuestionTask();
        task.setUserId(userId);
        task.setAppId(appId);
        task.setQuestionNum(questionNum);
        task.setOptionNum(optionNum);
        task.setStatus("waiting");
        save(task);
        return task.getId();
    }

    @Override
    public boolean markRunning(Long id) {
        QuestionTask task = new QuestionTask();
        task.setId(id);
        task.setStatus("running");
        task.setUpdateTime(new Date());
        return updateById(task);
    }

    @Override
    public boolean updateProgress(Long id, int progress) {
        return lambdaUpdate()
                .set(QuestionTask::getProgress, progress)
                .eq(QuestionTask::getId, id)
                .update();
    }

    @Override
    public boolean finish(Long id, List<Long> questionIds) {
        // ① 构造部分更新实体
        QuestionTask task = new QuestionTask();
        task.setId(id);
        task.setStatus("succeed");
        task.setProgress(100);
        task.setGenResult(JSONUtil.toJsonStr(questionIds));
        task.setUpdateTime(new Date());

        // ② 部分列更新
        return updateById(task);    // MyBatis-Plus 按主键 UPDATE
    }

    @Override
    public boolean fail(Long id, String execMessage) {
        QuestionTask task = new QuestionTask();
        task.setId(id);
        task.setStatus("failed");
        task.setExecMessage(execMessage);
        task.setUpdateTime(new Date());
        return updateById(task);
    }
}
