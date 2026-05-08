package com.inkwell.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppBeans {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    //WebClient -> Ye use hota hai dusre microservices ko call karne ke liye (HTTP API call)
}



