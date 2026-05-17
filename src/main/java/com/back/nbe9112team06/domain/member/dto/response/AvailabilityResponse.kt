package com.back.nbe9112team06.domain.member.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이메일 사용 가능 여부 응답 데이터")
data class AvailabilityResponse(

    @field:Schema(description = "이메일 사용 가능 여부 (true: 사용 가능, false: 중복)", example = "true")
    val available: Boolean,
)