package com.back.nbe9112team06.global.rq;


import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import com.back.nbe9112team06.global.security.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ✅ HTTP Request/Response 관련 유틸리티
 * - Controller 에서만 사용 권장 (계층 분리)
 * - 쿠키 설정, 인증된 사용자 조회 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Rq {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.name:accessToken}")
    private String cookieName;

    @Value("${app.cookie.max-age:3600}")
    private int cookieMaxAge;

    /**
     * ✅ 인증된 사용자 조회 (미인증 시 BusinessException 발생)
     * - Controller 에서 호출하여 401 응답 자동 처리
     */
    public Member getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Principal 타입 안전성 체크 + 패턴 매칭으로 캐스팅
        if (!(auth.getPrincipal() instanceof SecurityUser securityUser)) {
            log.warn("[Rq] Invalid principal type: {}",
                    auth.getPrincipal() != null ? auth.getPrincipal().getClass() : "null");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return new Member(securityUser.getId(), securityUser.getNickname());
    }

    /**
     * ✅ 로그인 여부 확인 (예외 없이 boolean 반환)
     * - 선택적 사용, 기본은 getActor() 예외 흐름 권장
     */
    public boolean isLogged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    public void issueAccessTokenCookie(String token) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge(cookieMaxAge);
        response.addCookie(cookie);
    }

    public void clearAccessTokenCookie() {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
    }
}