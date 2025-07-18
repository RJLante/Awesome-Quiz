package com.rd.backend.controller;

import com.rd.backend.common.BaseResponse;
import com.rd.backend.common.ErrorCode;
import com.rd.backend.common.ResultUtils;
import com.rd.backend.exception.BusinessException;
import com.rd.backend.exception.ThrowUtils;
import com.rd.backend.model.entity.QuestionTask;
import com.rd.backend.service.QuestionTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final QuestionTaskService taskService;

    /** 轮询任务进度 */
    @GetMapping("/{id}")
    public BaseResponse<QuestionTask> getTask(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        QuestionTask task = taskService.getById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        }
        return ResultUtils.success(task);
    }
}
