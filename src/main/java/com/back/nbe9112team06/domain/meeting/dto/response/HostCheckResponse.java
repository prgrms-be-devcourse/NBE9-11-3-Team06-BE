package com.back.nbe9112team06.domain.meeting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "방장 여부 확인 응답 DTO")
public record HostCheckResponse(
        @Schema(description = "방장 여부 (true: 방장, false: 일반 참가자)", example = "true")
        boolean isHost
) {}