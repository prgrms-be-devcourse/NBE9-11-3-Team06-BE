package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 회원 도메인 특화 오류 응답 (404, 409) 을 문서에 추가하는 메타 어노테이션
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "404",
        description = "존재하지 않는 회원 (MEMBER-001)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(
                        name = "memberNotFound",
                        value = """
                {
                  "type": "https://api.nbe9112team06.com/errors/member/001",
                  "title": "Not Found",
                  "status": 404,
                  "detail": "존재하지 않는 회원입니다.",
                  "errorCode": "MEMBER-001",
                  "timestamp": "2024-01-15T10:30:00Z"
                }
                """
                )
        )
)
@ApiResponse(
        responseCode = "409",
        description = "이메일 중복 (MEMBER-002)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(
                        name = "duplicateEmail",
                        value = """
                {
                  "type": "https://api.nbe9112team06.com/errors/member/002",
                  "title": "Conflict",
                  "status": 409,
                  "detail": "이미 등록된 이메일입니다.",
                  "errorCode": "MEMBER-002",
                  "timestamp": "2024-01-15T10:30:00Z"
                }
                """
                )
        )
)
public @interface MemberErrorResponses {}