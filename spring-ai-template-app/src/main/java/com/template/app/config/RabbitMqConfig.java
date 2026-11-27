package com.template.app.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "ai.exchange";
    public static final String QUEUE_DOC_PROCESS = "ai.doc.process";
    public static final String ROUTING_DOC_PROCESS = "ai.doc.process";

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue docProcessQueue() {
        return QueueBuilder.durable(QUEUE_DOC_PROCESS)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "ai.doc.dlq")
                .build();
    }

    @Bean
    public Queue docDlqQueue() {
        return QueueBuilder.durable("ai.doc.dlq").build();
    }

    @Bean
    public Binding docProcessBinding(Queue docProcessQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(docProcessQueue).to(aiExchange).with(ROUTING_DOC_PROCESS);
    }

    @Bean
    public Binding docDlqBinding(Queue docDlqQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(docDlqQueue).to(aiExchange).with("ai.doc.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
