package com.company.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.company.model.constant.RabbitConstant.PAYMENT_EXCHANGE;
import static com.company.model.constant.RabbitConstant.PAYMENT_QUEUE;
import static com.company.model.constant.RabbitConstant.PAYMENT_ROUTING_KEY;

@Configuration
public class RabbitMQConfig {

    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument(X_DEAD_LETTER_EXCHANGE, PAYMENT_EXCHANGE + ".dlx")
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, PAYMENT_ROUTING_KEY + ".dlq")
                .build();
    }

    @Bean
    public Queue paymentQueueDLQ() {
        return QueueBuilder.durable(PAYMENT_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public TopicExchange paymentExchange(){
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange(){
        return new TopicExchange(PAYMENT_EXCHANGE + ".dlx");
    }

    @Bean
    public Binding bindingPaymentQueue(Queue paymentQueue, TopicExchange paymentExchange){
        return BindingBuilder.bind(paymentQueue)
                .to(paymentExchange)
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding bindingStockUpdatedDLQ(Queue paymentQueueDLQ, TopicExchange deadLetterExchange){
        return BindingBuilder.bind(paymentQueueDLQ)
                .to(deadLetterExchange)
                .with(PAYMENT_ROUTING_KEY + ".dlq");
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());

        return rabbitTemplate;
    }

}
