package com.back.nbe9112team06.global.rq

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import com.back.nbe9112team06.global.security.SecurityUser
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component


/**
 * ✅ HTTP Request/Response 관련 유틸리티
 * - Controller 에서만 사용 권장 (계층 분리)
 * - 쿠키 설정, 인증된 사용자 조회 담당
 */
@Component
class Rq(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
    @Value("\${app.cookie.secure:true}") private val cookieSecure: Boolean,
    @Value("\${app.cookie.name:accessToken}") private val cookieName: String,
    @Value("\${app.cookie.max-age:3600}") private val cookieMaxAge: Int
) {
    private val logger = KotlinLogging.logger {}
    /**
     * ✅ 인증된 사용자 조회 (미인증 시 BusinessException 발생)
     * - Controller 에서 호출하여 401 응답 자동 처리
     */
    // property로 하기에는 Rq는 예외를 던진다.
    fun getActor(): Member {
            val auth = SecurityContextHolder.getContext().authentication
                ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

            if (!auth.isAuthenticated || auth is AnonymousAuthenticationToken) {
                throw BusinessException(ErrorCode.UNAUTHORIZED)
            }

        // 2. Principal 타입 안전성 체크 + 패턴 매칭으로 캐스팅
        val securityUser = auth.principal as? SecurityUser
            ?: run {
                logger.warn { "[Rq] Invalid principal type: ${auth.principal?.javaClass ?: "null"}" }
                throw BusinessException(ErrorCode.UNAUTHORIZED)
            }
            return Member(securityUser.id, securityUser.nickname)
        }
    /**
     * ✅ 로그인 여부 확인 (예외 없이 boolean 반환)
     * - 선택적 사용, 기본은 getActor() 예외 흐름 권장
     */

    fun isLogged(): Boolean {
            val auth = SecurityContextHolder.getContext().authentication
            return auth?.isAuthenticated == true && auth !is AnonymousAuthenticationToken
    }

    fun issueAccessTokenCookie(token: String) {
        Cookie(cookieName, token).apply {
            path ="/"
            isHttpOnly = true
            secure = cookieSecure
            setAttribute("SameSite", "Strict")
            maxAge = cookieMaxAge
        }.let{response.addCookie(it)}
    }

    fun clearAccessTokenCookie() {
        Cookie(cookieName, "").apply {
            path ="/"
            isHttpOnly = true
            secure = cookieSecure
            setAttribute("SameSite", "Strict")
            maxAge = 0
        }.let{response.addCookie(it)}
    }
}