package com.back.nbe9112team06.domain.member.dto

import com.back.nbe9112team06.domain.member.entity.TimezoneType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "회원가입 요청")
data class SignupRequest(

    @field:Schema(description = "이메일", example = "user@example.com")
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:Schema(description = "비밀번호 (8~20자)", example = "secure123!")
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    val password: String,

    @field:Schema(description = "닉네임 (2~20자)", example = "gildong")
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 20)
    val nickname: String,

    @field:Schema(
        description = "시간대",
        example = "ASIA_SEOUL",
        allowableValues = ["ASIA_SEOUL", "UTC", "AMERICA_NEW_YORK"],
    )
    @field:NotNull(message = "시간대는 필수입니다")
    val timezone: TimezoneType,
)