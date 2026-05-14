package com.back.nbe9112team06.domain.timeblock.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "참여자 시간표 삭제 요청 데이터")
@Getter
@NoArgsConstructor
public class TimeBlockDeleteRequest {

    @Schema(description = "참여자 이름 (게스트명)", example = "김철수", maxLength = 50)
    @NotBlank(message = "이름을 입력해주세요")
    private String guestName;

    @Schema(description = "참여자 인증용 비밀번호", example = "1234", maxLength = 20)
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String guestPassword;
}