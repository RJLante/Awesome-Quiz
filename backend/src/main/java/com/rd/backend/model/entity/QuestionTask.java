package com.rd.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 题目任务
 * @TableName question_task
 */
@TableName(value ="question_task")
@Data
public class QuestionTask implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 题目数量
     */
    private Integer questionNum;

    /**
     * 选项数量
     */
    private Integer optionNum;

    /**
     * waiting / running / succeed / failed
     */
    private String status;

    private Integer progress;

    /**
     * 题目内容
     */
    private String genResult;

    /**
     * 失败信息
     */
    private String execMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}