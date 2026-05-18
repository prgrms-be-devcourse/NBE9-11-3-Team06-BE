package com.back.nbe9112team06.global.security

import com.back.nbe9112team06.global.error.ErrorCode
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jsonMapper: JsonMapper
) : OncePerRequestFilter() {
    //TODO kotlin으로 변경시 아래 kotlin logging으로 변경
//    private val logger = KotlinLogging.logger {}

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith("/api/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.resolveTokenFromCookie()

        if (token != null) {
            try {
                val payload = jwtTokenProvider.getPayload(token)

                payload?.let {claims ->
                    // jjwt안에 박스된 타입은 자동으로 int 클래스 전환 X RequiredTypeException 발생
                    // 수동 Integer로 변환(Int -> Integer로 자동변환)Integer로 넣으면 인식 X
                    val id = claims["id", Int::class.javaObjectType] // 연산자 오버로딩 컨밴션 []
                    val nickname = claims["nickname", String::class.java]// claims 인터페이스에 get()으로 정의돼서 가능

                    val securityUser = SecurityUser(id, nickname)

                    // authorities 빈 리스트 — 역할 검증 없음
                    val auth: Authentication = UsernamePasswordAuthenticationToken(
                        securityUser,
                        null,
                        emptyList()
                    )
                    SecurityContextHolder.getContext().authentication = auth //setter 프로퍼티
                }
            } catch (e: ExpiredJwtException) {
                // 만료 토큰 → 필터에서 직접 응답 (filterChain 진행 안 함)
                logger.warn("[JWT] 만료된 토큰: uri=${request.requestURI}")// 추후에 수정
                response.writeErrorResponse(request, ErrorCode.TOKEN_EXPIRED)
                return
            }
        }
        filterChain.doFilter(request, response)
    }

    // 확장함수로 kotlin스럽게 변환
    // asSequence사용 이유 firstOrNull와 시퀸스 궁합이 좋다(조기종료)
    private fun HttpServletRequest.resolveTokenFromCookie(): String? {

        return cookies?.asSequence()
            ?.filter {it.name == "accessToken"}
            ?.map{ it.value }
            ?.firstOrNull{it.isNotBlank()}
    }

    // 확장함수는 맞지만  이미 필터에서 json을 주입받고 private이기에 재사용성에서도 의미X
    // 호출할때 json까지 넘겨줘야하는게 번거롭다.
    private fun HttpServletResponse.writeErrorResponse(
        request: HttpServletRequest,
        errorCode: ErrorCode
    ) {
        val pd = errorCode.toProblemDetail(
            errorCode.message,
            request.requestURI
        )
        contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        status = errorCode.status.value()
        characterEncoding = "UTF-8"
        writer.write(jsonMapper.writeValueAsString(pd))
    }
}