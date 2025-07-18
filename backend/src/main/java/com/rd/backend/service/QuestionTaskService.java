package com.rd.backend.service;

import com.rd.backend.model.entity.QuestionTask;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author rd
* @description 针对表【question_task(题目任务)】的数据库操作Service
* @createDate 2025-07-02 17:17:14
*/
public interface QuestionTaskService extends IService<QuestionTask> {

    /** 新建任务，返回 taskId */
    Long createTask(Long userId, Long appId, int questionNum, int optionNum);

    boolean updateProgress(Long id, int progress);

    /** 把任务标为 running */
    boolean markRunning(Long id);

    /** 成功结束，写入结果 id 列表 */
    boolean finish(Long id, List<Long> questionIds);

    /** 失败结束，写失败原因 */
    boolean fail(Long id, String execMessage);
}
