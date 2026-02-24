package com.example.corebanking.common.security;

import com.example.corebanking.global.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Gateway-Integrated Auth Filter
 *
 * Authentication Priority:
 * 1. X-User-UUID header (Verified by API Gateway)
 * 2. Authorization header (Direct JWT verification, bypassing Gateway)
 *
 * [Via Gateway] Client → Gateway(JWT Verification) → X-User-UUID attach → core-banking(Header Reference)
 * [Direct Access] Client → core-banking(JWT Verification)
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String GATEWAY_USER_HEADER = "X-User-UUID";
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userUuid = null;

        // 1: X-User-UUID header forwarded from API Gateway
        String gatewayUuid = request.getHeader(GATEWAY_USER_HEADER);
        if (StringUtils.hasText(gatewayUuid)) {
            userUuid = gatewayUuid;
            log.info("Authenticated via Gateway header - userUuid: {}", userUuid);
        }

        // 2: Direct JWT verification
        if (userUuid == null) {
            String token = resolveToken(request);
            if (token != null && jwtProvider.validateToken(token)) {
                userUuid = jwtProvider.getUserUuid(token);
                log.info("Authenticated via JWT token - userUuid: {}", userUuid);
            }
        }

        // Set authentication information in SecurityContext
        if (userUuid != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userUuid, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}