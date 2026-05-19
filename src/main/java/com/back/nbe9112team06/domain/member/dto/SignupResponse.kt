package com.back.nbe9112team06.domain.member.dto

import com.back.nbe9112team06.domain.member.entity.Member
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원가입 성공 응답")
data class SignupResponse(

    @field:Schema(description = "이메일")
    val email: String,

    @field:Schema(description = "닉네임")
    val nickname: String,
) {
    companion object {
        fun from(member: Member): SignupResponse =
            SignupResponse(
                email = member.email,
                nickname = member.nickname,
            )
    }
}