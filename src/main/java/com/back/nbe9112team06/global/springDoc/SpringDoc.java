package com.back.nbe9112team06.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API 서버", version = "beta", description = "API 서버 문서입니다."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SpringDoc {

    @Bean
    public GroupedOpenApi groupApiV1() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi groupController() {
        return GroupedOpenApi.builder()
                .group("home")
                .pathsToExclude("/api/**")
                .build();
    }
    // ✅ ProblemDetail 스키마를 components/schemas 에 등록 (한 번만 정의)
    @Bean
    public OpenAPI customizeOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSchemas("ProblemDetail", buildProblemDetailSchema()));
    }

    /**
     * RFC 9457 ProblemDetail 표준 스키마 정의
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">RFC 9457</a>
     */
    private Schema<Object> buildProblemDetailSchema() {
        return new Schema<>()
                .description("RFC 9457 표준 기반 오류 응답 객체")
                .type("object")
                .addProperties("type", new StringSchema()
                        .format("uri-reference")
                        .description("문제 유형을 식별하는 URI (예: /errors/member/002)"))
                .addProperties("title", new StringSchema()
                        .description("오류 유형에 대한 짧은 요약 (HTTP 상태 메시지)"))
                .addProperties("status", new IntegerSchema()
                        .description("HTTP 상태 코드 (예: 409)")
                        .example(409))
                .addProperties("detail", new StringSchema()
                        .description("이 특정 오류 발생에 대한 인간이 읽을 수 있는 설명")
                        .example("이미 등록된 이메일입니다."))
                .addProperties("instance", new StringSchema()
                        .format("uri-reference")
                        .description("이 특정 오류 발생을 식별하는 URI (선택사항)"))
                .addProperties("errorCode", new StringSchema()
                        .description("애플리케이션 고유 오류 코드 (예: MEMBER-002)")
                        .example("MEMBER-002"))
                .addProperties("timestamp", new StringSchema()
                        .format("date-time")
                        .description("오류 발생 시각 (ISO 8601)")
                        .example("2024-01-15T10:30:00Z"));
    }
}