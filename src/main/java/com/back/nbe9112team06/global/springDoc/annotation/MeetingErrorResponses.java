package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 모임 도메인 특화 오류 응답 (400, 403, 404, 409) 을 문서에 추가하는 메타 어노테이션
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "400",
        description = "확정되지 않은 모임 또는 참여자 없음 (MEETING-004, MEETING-005)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = {
                        @ExampleObject(name = "notConfirmed", value = """
                {"errorCode":"MEETING-004","detail":"확정되지 않은 모임입니다."}
                """),
                        @ExampleObject(name = "noParticipants", value = """
                {"errorCode":"MEETING-005","detail":"참여자가 없는 모임은 일정을 확정할 수 없습니다."}
                """)
                }
        )
)
@ApiResponse(
        responseCode = "403",
        description = "방장 권한 없음 (MEETING-002)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "notMeetingHost", value = """
                {"errorCode":"MEETING-002","detail":"해당 모임의 호스트(방장)만 가능합니다."}
                """)
        )
)
@ApiResponse(
        responseCode = "404",
        description = "존재하지 않는 모임 (MEETING-001)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "meetingNotFound", value = """
                {"errorCode":"MEETING-001","detail":"존재하지 않는 모임입니다."}
                """)
        )
)
@ApiResponse(
        responseCode = "409",
        description = "이미 확정된 모임 (MEETING-003)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "alreadyConfirmed", value = """
                {"errorCode":"MEETING-003","detail":"이미 확정된 모임입니다."}
                """)
        )
)
public @interface MeetingErrorResponses {}