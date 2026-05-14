package com.back.nbe9112team06.global.springDoc.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

/**
 * 타임테이블 도메인 특화 오류 응답 (400, 404) 을 문서에 추가하는 메타 어노테이션
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "400",
        description = "타임블록 데이터 없음 (TIMETABLE-001)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "noTimeBlocks", value = """
                {"errorCode":"TIMETABLE-001","detail":"타임블록 데이터가 없어 시간표를 생성할 수 없습니다."}
                """)
        )
)
@ApiResponse(
        responseCode = "404",
        description = "모임 조회 실패 (MEETING-001)",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(ref = "#/components/schemas/ProblemDetail"),
                examples = @ExampleObject(name = "meetingNotFound", value = """
                {"errorCode":"MEETING-001","detail":"존재하지 않는 모임입니다."}
                """)
        )
)
public @interface TimeTableErrorResponses {}