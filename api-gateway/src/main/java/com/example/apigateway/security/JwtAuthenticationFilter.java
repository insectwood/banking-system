package com.example.apigateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * API Gateway JWT Authentication Filter
 * Client → Gateway (JWT Verification + UUID Extraction) → core-banking (Header reference only)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtProperties jwtProperties;
    private Key key;

    private static final List<String> PERMIT_PATHS = List.of(
            "/api/v1/auth/",
            "/actuator/"
    );

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // /Auth Skip
        if (isPermitPath(path)) {
            return chain.filter(exchange);
        }

        // OPTIONS (CORS preflight) Skip
        if ("OPTIONS".equals(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        // Authorization / Extract token form header
        String token = resolveToken(request);

        if (token == null) {
            return unauthorizedResponse(exchange, "Missing Authorization header");
        }

        // jwt authentication + userUuid Extract
        String userUuid;
        try {
            userUuid = validateAndExtractUuid(token);
        } catch (ExpiredJwtException e) {
            return unauthorizedResponse(exchange, "Token expired");
        } catch (Exception e) {
            return unauthorizedResponse(exchange, "Invalid token");
        }

        // attache X-User-UUID header, and forward the request
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-UUID", userUuid)
                .build();

        log.debug("JWT verified - userUuid: {}, path: {}", userUuid, path);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPermitPath(String path) {
        return PERMIT_PATHS.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String validateAndExtractUuid(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (claims.getExpiration().before(new Date())) {
            throw new ExpiredJwtException(null, claims, "Token expired");
        }

        return claims.getSubject(); // userUuid
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}