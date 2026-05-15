package com.inkwell.auth_service.config;

import com.inkwell.auth_service.security.JwtAuthenticationFilter;
import com.inkwell.auth_service.security.OAuth2SuccessHandler;
import com.inkwell.auth_service.service.OAuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor //Constructor automatically banata hai for final fields
public class SecurityConfig {


    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuthUserService oauthUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    @Value("${app.cors.allowed-origin-patterns:http://localhost:5173,https://*.vercel.app}")
    private String allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable()) // Explicitly disable here as Gateway handles it
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/register-admin",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/validate",
                                "/api/auth/oauth2/success",
                                "/api/auth/internal/**",
                                "/api/auth/swagger-ui.html",
                                "/api/auth/swagger-ui/**",
                                "/api/auth/v3/api-docs/**",
                                "/api/auth/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/oauth2/**",
                                "/login/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo.userService(oauthUserService))
                .successHandler(oAuth2SuccessHandler));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
