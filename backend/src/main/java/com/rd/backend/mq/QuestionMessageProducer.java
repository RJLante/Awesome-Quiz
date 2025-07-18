package com.rd.backend.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class QuestionMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(QuestionMqConstant.QUESTION_EXCHANGE_NAME,
                QuestionMqConstant.QUESTION_ROUTING_KEY, message);
    }
}
