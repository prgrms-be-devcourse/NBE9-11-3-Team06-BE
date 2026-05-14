package com.back.nbe9112team06.domain.member.dto;

import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "비밀번호 (8~20자)", example = "secure123!")
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @Schema(description = "닉네임 (2~20자)", example = "gildong")
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20)
        String nickname,

        @Schema(
                description = "시간대",
                example = "ASIA_SEOUL",
                allowableValues = {"ASIA_SEOUL", "UTC", "AMERICA_NEW_YORK"}
        )
        @NotNull(message = "시간대는 필수입니다")
        TimezoneType timezone
) {}


