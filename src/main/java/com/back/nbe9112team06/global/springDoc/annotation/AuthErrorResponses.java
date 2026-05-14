package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 인증/인가 관련 오류 응답 (401, 403) 을 문서에 추가하는 메타 어노테이션
 * - 토큰 없음/위조/만료/세션없음 (AUTH-001/002/003/006)
 * - 인가 실패 (AUTH-005)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = {
                        @ExampleObject(
                                name = "tokenMissing",
                                summary = "토큰 없음",
                                value = """
                                {
                                  "type": "https://api.nbe9112team06.com/errors/auth/001",
                                  "title": "Unauthorized",
                                  "status": 401,
                                  "detail": "인증 토큰이 없습니다.",
                                  "errorCode": "AUTH-001",
                                  "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                        ),
                        @ExampleObject(
                                name = "tokenInvalid",
                                summary = "토큰 위조/형식 오류",
                                value = """
                                {
                                  "type": "https://api.nbe9112team06.com/errors/auth/002",
                                  "title": "Unauthorized",
                                  "status": 401,
                                  "detail": "인증 토큰이 유효하지 않습니다.",
                                  "errorCode": "AUTH-002",
                                  "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                        ),
                        @ExampleObject(
                                name = "tokenExpired",
                                summary = "토큰 만료",
                                value = """
                                {
                                  "type": "https://api.nbe9112team06.com/errors/auth/003",
                                  "title": "Unauthorized",
                                  "status": 401,
                                  "detail": "인증 토큰이 만료되었습니다.",
                                  "errorCode": "AUTH-003",
                                  "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                        ),
                        @ExampleObject(
                                name = "unauthorized",
                                summary = "인증 세션 없음 (Rq.getActor 실패)",
                                value = """
                                {
                                  "type": "https://api.nbe9112team06.com/errors/auth/006",
                                  "title": "Unauthorized",
                                  "status": 401,
                                  "detail": "인증이 필요합니다.",
                                  "errorCode": "AUTH-006",
                                  "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                        )
                }
        )
)
@ApiResponse(
        responseCode = "403",
        description = "인가 실패: 접근 권한 없음 (AUTH-005)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(
                        name = "accessDenied",
                        value = """
                        {
                          "type": "https://api.nbe9112team06.com/errors/auth/005",
                          "title": "Forbidden",
                          "status": 403,
                          "detail": "접근 권한이 없습니다.",
                          "errorCode": "AUTH-005",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                )
        )
)
public @interface AuthErrorResponses {}