package com.back.nbe9112team06.domain.timetable.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "시간 슬롯별 참여자 정보")
public record TimeResponse(

        @Schema(description = "가능 시간 (HH:mm)", example = "09:00")
        LocalTime time,

        @Schema(description = "해당 시간에 가능한 참여자 이름 목록")
        List<String> participants,

        @Schema(description = "해당 시간에 가능한 참여자 수", example = "2")
        int count

) {
}