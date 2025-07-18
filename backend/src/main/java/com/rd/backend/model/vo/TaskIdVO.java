package com.rd.backend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用异步任务返回对象
 * 仅包含 taskId，便于前端轮询或 WebSocket 获取进度
 */
@Data
public class TaskIdVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异步任务主键 */
    private Long taskId;
}
