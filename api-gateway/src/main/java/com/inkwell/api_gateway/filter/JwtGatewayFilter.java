package com.inkwell.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Value("${auth.jwt.secret}")
    private String secret;

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/auth/register",
            "/auth/register-admin",
            "/auth/login",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/validate",
            "/auth/oauth2",
            "/newsletter/subscribe",
            "/newsletter/confirm",
            "/newsletter/unsubscribe",
            "/media/upload",
            "/media/files/",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/oauth2/",
            "/login/",
            "/gateway/",
            "/eureka"
    );

    private static final List<String> GET_ONLY_PUBLIC_PREFIXES = List.of(
            "/posts/published",
            "/posts/slug",
            "/posts/category",
            "/posts/tag",
            "/categories",
            "/tags",
            "/media/files/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(exchange.getRequest().getMethod(), path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
            String userId = String.valueOf(claims.get("userId"));
            String role = String.valueOf(claims.get("role"));

            ServerWebExchange mutated = exchange.mutate()
                    .request(req -> req.headers(headers -> {
                        headers.set("X-User-Id", userId);
                        headers.set("X-User-Role", role);
                    }))
                    .build();

            return chain.filter(mutated);
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -2;
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return HttpMethod.GET.equals(method) && GET_ONLY_PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
