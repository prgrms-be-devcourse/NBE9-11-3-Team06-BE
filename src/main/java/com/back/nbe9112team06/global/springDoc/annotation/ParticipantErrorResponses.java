package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 참가자 도메인 특화 오류 응답 (400, 404, 409) 을 문서에 추가하는 메타 어노테이션
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "400",
        description = "유효성 검사 실패 (PARTICIPANT-001)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "validationFailed", value = """
                {"errorCode":"PARTICIPANT-001","detail":"참가자 이름 또는 비밀번호가 비어있습니다."}
                """)
        )
)
@ApiResponse(
        responseCode = "404",
        description = "모임방 또는 참가자 조회 실패 (MEETING-001, PARTICIPANT-002)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = {
                        @ExampleObject(name = "meetingNotFound", value = """
                        {"errorCode":"MEETING-001","detail":"존재하지 않는 모임입니다."}
                        """),
                        @ExampleObject(name = "participantNotFound", value = """
                        {"errorCode":"PARTICIPANT-002","detail":"존재하지 않는 참가자입니다."}
                        """)
                }
        )
)
@ApiResponse(
        responseCode = "409",
        description = "중복 참가 제한 (선택사항, 현재는 허용) (PARTICIPANT-003)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "duplicateParticipant", value = """
                {"errorCode":"PARTICIPANT-003","detail":"이미 참여한 참가자입니다."}
                """)
        )
)
public @interface ParticipantErrorResponses {}