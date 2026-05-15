package com.back.nbe9112team06.domain.timetable.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "시간 슬롯별 참여자 정보")

data class TimeResponse(
    @field:Schema(
        description = "가능 시간 (HH:mm)",
        example = "09:00"
    ) @param:Schema(
        description = "가능 시간 (HH:mm)",
        example = "09:00"
    ) val time: LocalTime,

    @field:Schema(description = "해당 시간에 가능한 참여자 이름 목록") @param:Schema(
        description = "해당 시간에 가능한 참여자 이름 목록"
    ) val participants: List<String>,

    @field:Schema(
        description = "해당 시간에 가능한 참여자 수",
        example = "2"
    ) @param:Schema(description = "해당 시간에 가능한 참여자 수", example = "2") val count: Int

) 