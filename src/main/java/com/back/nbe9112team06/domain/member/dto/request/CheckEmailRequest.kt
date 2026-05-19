package com.back.nbe9112team06.domain.member.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "이메일 중복 체크 요청 데이터")
data class CheckEmailRequest(

    @Schema(description = "확인할 이메일 주소", example = "user@example.com")
    @NotBlank
    @Email
    val email: String,
)