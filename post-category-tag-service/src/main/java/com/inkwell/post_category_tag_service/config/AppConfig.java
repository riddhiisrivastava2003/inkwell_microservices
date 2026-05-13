package com.inkwell.post_category_tag_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class AppConfig {
    public static final String POST_EVENTS_EXCHANGE = "inkwell.post.events";
    public static final String NEW_POST_QUEUE = "inkwell.newsletter.new-post.queue";
    public static final String NEW_POST_ROUTING_KEY = "post.published";

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}



