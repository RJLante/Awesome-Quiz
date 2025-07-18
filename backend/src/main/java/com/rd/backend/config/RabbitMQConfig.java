package com.rd.backend.config;

import com.rd.backend.mq.QuestionMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 资源声明
 */
@Configuration
public class RabbitMQConfig {

    /** 直连交换机 */
    @Bean
    public DirectExchange questionExchange() {
        return ExchangeBuilder
                .directExchange(QuestionMqConstant.QUESTION_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    /** 队列 */
    @Bean
    public Queue questionQueue() {
        return QueueBuilder
                .durable(QuestionMqConstant.QUESTION_QUEUE_NAME)
                .build();
    }

    /** 绑定 */
    @Bean
    public Binding questionBinding() {
        return BindingBuilder
                .bind(questionQueue())
                .to(questionExchange())
                .with(QuestionMqConstant.QUESTION_ROUTING_KEY);
    }
}
