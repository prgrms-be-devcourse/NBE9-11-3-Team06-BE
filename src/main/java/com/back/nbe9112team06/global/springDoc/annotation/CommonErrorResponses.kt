package com.back.nbe9112team06.global.springDoc.annotation

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

/**
 * 공통 오류 응답 (400, 500) 을 문서에 자동 추가하는 메타 어노테이션
 * 사용 예시:
 * `publicApiResponse<Void> someApi() { ... } `
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ApiResponse(
    responseCode = "400",
    description = "요청 파라미터 검증 실패 (COMMON-009)",
    content = [Content(
        mediaType = "application/problem+json",
        schema = Schema(ref = "#/components/schemas/ProblemDetail")
    )]
)
@ApiResponse(
    responseCode = "500",
    description = "서버 내부 오류 (COMMON-001)",
    content = [Content(
        mediaType = "application/problem+json",
        schema = Schema(ref = "#/components/schemas/ProblemDetail")
    )]
)
annotation class CommonErrorResponses 
