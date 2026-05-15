package com.back.nbe9112team06.domain.timeblock.dto.response

import com.back.nbe9112team06.domain.timeblock.dto.TimeRangeResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "참여자별 가능 시간 범위 응답 데이터")
data class ParticipantsScheduleResponse(

    @field:Schema(description = "참여자 이름", example = "김철수")
    val name: String,

    @field:Schema(description = "연속된 가능 시간 범위 목록")
    val availableTimeRanges: List<TimeRangeResponse>,
)