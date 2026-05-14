package com.back.nbe9112team06.domain.timetable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "날짜별 가능 시간 정보")
public record DateResponse(

        @Schema(description = "가능 날짜 (yyyy-MM-dd)", example = "2024-05-20")
        LocalDate availableDate,

        @Schema(description = "해당 날짜의 시간 슬롯별 정보 목록")
        List<TimeResponse> availableTimeInfos

) {
}

