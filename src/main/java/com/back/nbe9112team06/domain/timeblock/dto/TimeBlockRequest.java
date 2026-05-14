package com.back.nbe9112team06.domain.timeblock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "참여자 시간표 등록 요청 데이터")
@Getter
@NoArgsConstructor
public class TimeBlockRequest {

    @Schema(description = "참여자 이름 (게스트명)", example = "김철수", maxLength = 50)
    @NotBlank(message = "이름을 입력해주세요")
    private String guestName;

    @Schema(description = "참여자 인증용 비밀번호", example = "1234", maxLength = 20)
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String guestPassword;

    @Schema(description = "가능한 시간 목록 (30분 단위, 'yyyy-MM-dd HH:mm' 형식)",
            example = "[\"2026-04-20 14:00\", \"2026-04-20 14:30\", \"2026-04-21 10:00\"]")
    @NotEmpty(message = "가능한 시간을 선택해주세요")
    private List<String> availableDateTimes;
}