package com.back.nbe9112team06.global.security

import com.back.nbe9112team06.global.error.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jsonMapper: JsonMapper
) {
    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { request: HttpServletRequest, response: HttpServletResponse, ex: AuthenticationException? ->
            // 쿠키 존재 여부로 토큰 없음 vs 위조 구분
            val hasTokenCookie = request.cookies?.any { it.name == "accessToken" } ?: false
            val errorCode = if (hasTokenCookie) ErrorCode.TOKEN_INVALID else ErrorCode.TOKEN_MISSING
            response.writeProblemDetail(request, errorCode)
        }
    }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { request: HttpServletRequest, response: HttpServletResponse, ex: AccessDeniedException? ->
            response.writeProblemDetail(request, ErrorCode.ACCESS_DENIED)
        }
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/h2-console/**", permitAll)
                authorize(HttpMethod.GET, "/api/auth/me", authenticated)
                authorize(HttpMethod.POST, "/api/auth/logout", authenticated)
                authorize(HttpMethod.POST, "/api/meetings", authenticated)
                authorize(HttpMethod.POST, "/api/meetings/*/confirm", authenticated)
                authorize(HttpMethod.DELETE, "/api/members", authenticated)
                authorize(HttpMethod.DELETE, "/api/meetings/*/confirm", authenticated)

                authorize(anyRequest, permitAll)
            }
            csrf { disable() }
            cors { configurationSource = corsConfigurationSource() }
            headers {
                frameOptions { sameOrigin = true }
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>( // 컨테이너나 빈 등록X new로 직접 생성하여 중복 X
                JwtAuthenticationFilter(jwtTokenProvider, jsonMapper)
            )
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            exceptionHandling {
                authenticationEntryPoint = authenticationEntryPoint()
                accessDeniedHandler = accessDeniedHandler()
            }
        }
        return http.build()
    }

    private fun HttpServletResponse.writeProblemDetail(
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

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins =
                listOf("http://localhost:3000")
            allowedMethods =
                listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}
