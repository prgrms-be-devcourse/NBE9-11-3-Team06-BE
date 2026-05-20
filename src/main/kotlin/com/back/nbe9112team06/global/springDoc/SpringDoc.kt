package com.back.nbe9112team06.global.springDoc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(info = Info(title = "API 서버", version = "beta", description = "API 서버 문서입니다."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
class SpringDoc {
    @Bean
    fun groupApiV1(): GroupedOpenApi = GroupedOpenApi.builder()
            .group("api")
            .pathsToMatch("/api/**")
            .build()

    @Bean
    fun groupController(): GroupedOpenApi = GroupedOpenApi.builder()
            .group("home")
            .pathsToExclude("/api/**")
            .build()

    // ✅ ProblemDetail 스키마를 components/schemas 에 등록 (한 번만 정의)
    @Bean
    fun customizeOpenAPI(): OpenAPI =OpenAPI().apply{
            components = Components().apply {
                addSchemas("ProblemDetail", buildProblemDetailSchema())
            }
    }

    /**
     * RFC 9457 ProblemDetail 표준 스키마 정의
     * @see [RFC 9457](https://datatracker.ietf.org/doc/html/rfc9457)
     */
    //TODO addProperty는 deprecated addProperty로 변경
    private fun buildProblemDetailSchema(): Schema<Any> = Schema<Any>().apply {
        description = "RFC 9457 표준 기반 오류 응답 객체"
        type = "object"
        addProperty("type", StringSchema().apply {
            format = "uri-reference"
            description = "문제 유형을 식별하는 URI (예: /errors/member/002)"
        })
        addProperty("title", StringSchema().apply {
            description = "오류 유형에 대한 짧은 요약 (HTTP 상태 메시지)"
        })
        addProperty("status", IntegerSchema().apply {
            description = "HTTP 상태 코드 (예: 409)"
            example = 409
        })
        addProperty("detail", StringSchema().apply {
            description = "이 특정 오류 발생에 대한 인간이 읽을 수 있는 설명"
            example = "이미 등록된 이메일입니다."
        })
        addProperty("instance", StringSchema().apply {
            format = "uri-reference"
            description = "이 특정 오류 발생을 식별하는 URI (선택사항)"
        })
        addProperty("errorCode", StringSchema().apply {
            description = "애플리케이션 고유 오류 코드 (예: MEMBER-002)"
            example = "MEMBER-002"
        })
        addProperty("timestamp", StringSchema().apply {
            format = "date-time"
            description = "오류 발생 시각 (ISO 8601)"
            example = "2024-01-15T10:30:00Z"
        })
    }
}