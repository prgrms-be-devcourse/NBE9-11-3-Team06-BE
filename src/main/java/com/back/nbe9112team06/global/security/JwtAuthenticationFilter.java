package com.back.nbe9112team06.global.security;

import com.back.nbe9112team06.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JsonMapper jsonMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveTokenFromCookie(request);

        if (token != null) {
            try {
                Claims payload = jwtTokenProvider.getPayload(token);

                if (payload != null) {
                    int id = payload.get("id", Integer.class);
                    String nickname = payload.get("nickname", String.class);

                    SecurityUser securityUser = new SecurityUser(id, nickname);

                    // authorities 빈 리스트 — 역할 검증 없음
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            securityUser,
                            null,
                            List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException e) {
                // 만료 토큰 → 필터에서 직접 응답 (filterChain 진행 안 함)
                log.warn("[JWT] 만료된 토큰: uri={}", request.getRequestURI());
                writeErrorResponse(response, request, ErrorCode.TOKEN_EXPIRED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> "accessToken".equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst()
                .orElse(null);
    }
    private void writeErrorResponse(HttpServletResponse response,
                                    HttpServletRequest request,
                                    ErrorCode errorCode) throws IOException {
        ProblemDetail pd = errorCode.toProblemDetail(
                errorCode.getMessage(),
                request.getRequestURI()
        );
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(pd));
    }
}