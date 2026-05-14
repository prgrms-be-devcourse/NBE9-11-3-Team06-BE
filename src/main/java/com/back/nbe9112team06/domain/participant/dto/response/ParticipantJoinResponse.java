package com.back.nbe9112team06.domain.participant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임방 참가 성공 응답 데이터")
public record ParticipantJoinResponse(

        @Schema(description = "발급된 참가자 고유 ID", example = "1")
        Integer participantId,

        @Schema(description = "참가자 표시 이름 (게스트명)", example = "홍길동")
        String guestName

) {
}