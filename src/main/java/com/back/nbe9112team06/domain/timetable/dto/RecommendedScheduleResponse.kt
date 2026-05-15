package com.back.nbe9112team06.domain.timetable.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
@JvmRecord
@Schema(description = "추천 일정 응답 데이터")
data class RecommendedScheduleResponse(
    @field:Schema(
        description = "추천 날짜 (yyyy-MM-dd)",
        example = "2024-05-20"
    )
    val date: LocalDate,

    @field:Schema(
        description = "시작 시간 (HH:mm)",
        example = "09:00"
    )
    val startTime: LocalTime,

    @field:Schema(
        description = "종료 시간 (HH:mm)",
        example = "10:00"
    )
    val endTime: LocalTime,

    @field:Schema(
        description = "참여 가능한 인원 수",
        example = "2"
    )
    val availableCount: Int

) 