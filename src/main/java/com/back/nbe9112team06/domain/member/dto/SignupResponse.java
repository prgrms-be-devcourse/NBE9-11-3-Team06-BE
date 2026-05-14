package com.back.nbe9112team06.domain.member.dto;

import com.back.nbe9112team06.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 성공 응답")
public record SignupResponse(
        @Schema(description = "이메일")
        String email,

        @Schema(description = "닉네임")
        String nickname

) {
        public static  SignupResponse from(Member member){
                return new SignupResponse(
                        member.getEmail(),
                        member.getNickname()
                );
        }
}