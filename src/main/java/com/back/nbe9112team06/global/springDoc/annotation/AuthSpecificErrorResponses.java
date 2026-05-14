package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 로그인 실패 특화 오류 응답 (AUTH-004)
 * <p>
 * ⚠️ 이 어노테이션은 로그인 엔드포인트 (/api/auth/login) 전용입니다.
 * 다른 인증 필요 엔드포인트는 {@link AuthErrorResponses} 를 사용하세요.
 * <p>
 * 보안 원칙: 이메일/비밀번호 중 어느 것이 틀렸는지 구분하지 않고 통일된 응답 반환
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "401",
        description = "로그인 실패: 이메일 또는 비밀번호 불일치 (AUTH-004)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(
                        name = "invalidCredentials",
                        value = """
                        {
                          "type": "https://api.nbe9112team06.com/errors/auth/004",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "이메일 또는 비밀번호가 올바르지 않습니다.",
                          "errorCode": "AUTH-004",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                )
        )
)
public @interface AuthSpecificErrorResponses {}