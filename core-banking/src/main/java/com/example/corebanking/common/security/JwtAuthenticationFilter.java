package com.example.corebanking.common.security;

import com.example.corebanking.global.security.JwtProvider;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract token from header
        String token = resolveToken(request);

        // 2. Verify validity with validateToken
        if (token != null && jwtProvider.validateToken(token)) {

            // 3. Extract identifier with getUserUuid
            String userUuid = jwtProvider.getUserUuid(token);

            // 4. Create Spring Security authentication object and store in Context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userUuid, null, Collections.emptyList());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
