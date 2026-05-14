package com.back.nbe9112team06.domain.timeblock.dto.response;

import com.back.nbe9112team06.domain.timeblock.dto.TimeRangeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "참여자별 가능 시간 범위 응답 데이터")
public record ParticipantsScheduleResponse(

        @Schema(description = "참여자 이름", example = "김철수")
        String name,

        @Schema(description = "연속된 가능 시간 범위 목록")
        List<TimeRangeResponse> availableTimeRanges

) {
}