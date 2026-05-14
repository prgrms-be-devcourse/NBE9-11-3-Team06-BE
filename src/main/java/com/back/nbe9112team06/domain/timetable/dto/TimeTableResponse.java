package com.back.nbe9112team06.domain.timetable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "모임 시간표 응답 데이터")
public record TimeTableResponse(

        @Schema(description = "날짜별 가능 시간 정보 목록")
        List<DateResponse> availableDateTimes

) {
}