package com.back.nbe9112team06.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내부 반환용")
public record LoginResult(
        String accessToken,   // Controller가 쿠키로 발급 후 버림
        int memberId,
        String nickname
) {}