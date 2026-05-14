package com.back.nbe9112team06.domain.meeting.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MeetingCreateRequest(
        @Schema(description = "모임 제목", example = "팀 회의")
        @NotBlank(message = "모임명은 필수입니다.")
        String title,

        @Schema(description = "가능한 날짜 목록", example = "[\"2026-04-20\", \"2026-04-21\"]")
        @NotNull(message = "날짜 선택은 필수입니다")
        @NotEmpty(message = "최소 1개의 날짜를 선택해주세요.")
        List<LocalDate> dates,

        @Schema(description = "회의 시간 (분 단위)", example = "60", minimum = "30", maximum = "180")
        @NotNull(message = "최소 회의 시간은 필수입니다.")
        @Min(value = 30, message = "회의 시간은 30 분 이상이어야 합니다.")
        Integer duration,

        @Schema(description = "모임 카테고리", example = "PROJECT", allowableValues = {"PROJECT", "STUDY", "INTERVIEW"})
        @NotBlank(message = "모임 성격은 필수입니다.")
        String category
) {

}

