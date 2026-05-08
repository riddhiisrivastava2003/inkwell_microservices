package com.inkwell.newsletter_subscription_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String POST_EVENTS_EXCHANGE = "inkwell.post.events";
    public static final String NEW_POST_QUEUE = "inkwell.newsletter.new-post.queue";
    public static final String NEW_POST_ROUTING_KEY = "post.published";

    @Bean
    public TopicExchange postEventsExchange() {
        return new TopicExchange(POST_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue newPostQueue() {
        return new Queue(NEW_POST_QUEUE, true);
    }

    @Bean
    public Binding newPostBinding(Queue newPostQueue, TopicExchange postEventsExchange) {
        return BindingBuilder.bind(newPostQueue).to(postEventsExchange).with(NEW_POST_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
