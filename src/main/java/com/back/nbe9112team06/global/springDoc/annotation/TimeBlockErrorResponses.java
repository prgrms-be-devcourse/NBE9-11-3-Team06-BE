package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 타임블록 도메인 특화 오류 응답 (400, 404, 409) 을 문서에 추가하는 메타 어노테이션
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "400",
        description = "유효성 검사 실패 (TIMEBLOCK-001 ~ 004)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = {
                        @ExampleObject(name = "invalidDateTimeFormat", value = """
                        {"errorCode":"TIMEBLOCK-001","detail":"올바른 날짜 형식이 아닙니다. (yyyy-MM-dd HH:mm)"}
                        """),
                        @ExampleObject(name = "notThirtyMinuteUnit", value = """
                        {"errorCode":"TIMEBLOCK-002","detail":"30분 단위 시간이 아닙니다."}
                        """),
                        @ExampleObject(name = "pastDateTime", value = """
                        {"errorCode":"TIMEBLOCK-003","detail":"현재 날짜보다 과거 날짜는 선택할 수 없습니다."}
                        """),
                        @ExampleObject(name = "duplicateTimeSelection", value = """
                        {"errorCode":"TIMEBLOCK-004","detail":"시간 선택이 중복되었습니다."}
                        """)
                }
        )
)
@ApiResponse(
        responseCode = "404",
        description = "모임 또는 참여자 조회 실패 (MEETING-001, PARTICIPANT-002)",
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
        description = "중복 시간표 등록 (TIMEBLOCK-005)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "duplicateTimeBlock", value = """
                {"errorCode":"TIMEBLOCK-005","detail":"시간표가 이미 등록되었습니다."}
                """)
        )
)
public @interface TimeBlockErrorResponses {}