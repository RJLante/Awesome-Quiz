package com.rd.backend.mq;

public interface QuestionMqConstant {

    /** 交换机 */
    public static final String QUESTION_EXCHANGE_NAME = "question_exchange";

    /** 队列 */
    public static final String QUESTION_QUEUE_NAME = "question_generate_queue";

    /** 路由键 */
    public static final String QUESTION_ROUTING_KEY = "question.generate";
}
