package com.back.nbe9112team06.domain.participant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "모임방 비회원 참가 요청 데이터")
public record ParticipantJoinRequest(

        @Schema(description = "참가자 표시 이름 (게스트명)", example = "홍길동", maxLength = 50)
        @NotBlank(message = "참가자 이름은 필수입니다.")
        String guestName,

        @Schema(description = "참가자 인증용 비밀번호 (향후 참가자 인증에 활용)", example = "1234", maxLength = 20)
        @NotBlank(message = "참가자 비밀번호는 필수입니다.")
        String guestPassword

) {
}

