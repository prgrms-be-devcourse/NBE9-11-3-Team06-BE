package com.back.nbe9112team06.domain.timeblock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "연속된 가능 시간 범위 데이터")
public record TimeRangeResponse(

        @Schema(description = "날짜 (yyyy-MM-dd)", example = "2026-04-20")
        LocalDate date,

        @Schema(description = "시작 시간 (HH:mm)", example = "14:00")
        LocalTime startTime,

        @Schema(description = "종료 시간 (HH:mm)", example = "15:30")
        LocalTime endTime

) {
}