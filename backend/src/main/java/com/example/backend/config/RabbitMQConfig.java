package com.example.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String VIDEO_UPLOAD_QUEUE_JSON = "video.upload.json";
    public static final String VIDEO_UPLOAD_QUEUE_PROTOBUF = "video.upload.protobuf";

    // Exchange
    public static final String VIDEO_UPLOAD_EXCHANGE = "video.upload.exchange";

    // Routing keys
    public static final String ROUTING_KEY_JSON = "upload.json";
    public static final String ROUTING_KEY_PROTOBUF = "upload.protobuf";

    @Bean
    public Queue jsonQueue() {
        return new Queue(VIDEO_UPLOAD_QUEUE_JSON, true);
    }

    @Bean
    public Queue protobufQueue() {
        return new Queue(VIDEO_UPLOAD_QUEUE_PROTOBUF, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(VIDEO_UPLOAD_EXCHANGE);
    }

    @Bean
    public Binding jsonBinding(Queue jsonQueue, TopicExchange exchange) {
        return BindingBuilder.bind(jsonQueue).to(exchange).with(ROUTING_KEY_JSON);
    }

    @Bean
    public Binding protobufBinding(Queue protobufQueue, TopicExchange exchange) {
        return BindingBuilder.bind(protobufQueue).to(exchange).with(ROUTING_KEY_PROTOBUF);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
