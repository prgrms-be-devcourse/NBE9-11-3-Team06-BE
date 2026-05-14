package com.back.nbe9112team06.domain.timetable.controller;

import com.back.nbe9112team06.domain.timetable.dto.RecommendedScheduleResponse;
import com.back.nbe9112team06.domain.timetable.dto.TimeTableResponse;
import com.back.nbe9112team06.domain.timetable.service.TimeTableService;
import com.back.nbe9112team06.global.response.ApiResponse;
import com.back.nbe9112team06.global.springDoc.annotation.CommonErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.MeetingErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.TimeTableErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.back.nbe9112team06.global.springDoc.example.TimeTableApiExamples.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
@Tag(name = "TimeTables", description = "시간표 집계 및 조회 API")
public class TimeTableController {

    private final TimeTableService timeTableService;

    @GetMapping("/{meetingId}/timetable")
    @Operation(
            summary = "시간표 조회 (자동 집계 포함)",
            description = """
            특정 모임의 시간표를 조회합니다.
            - 조회 시점에 자동으로 `aggregate()` 를 실행하여 최신 데이터로 갱신
            - 날짜별 (`availableDate`) 로 그룹화되어 반환
            - 각 시간 슬롯별 참여자 목록 (`participants`) 과 인원수 (`count`) 포함
            - 인증 불필요 (permitAll) - 공유용 링크 제공 목적
            """
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @TimeTableErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "시간표 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(name = "success", value = GET_TIMETABLE_SUCCESS_JSON),
                            @ExampleObject(name = "empty", value = GET_TIMETABLE_EMPTY_JSON)
                    }
            )
    )
    public ApiResponse<TimeTableResponse> getTimeTable(@PathVariable Integer meetingId) {
        timeTableService.aggregate(meetingId);
        return new ApiResponse<>("200-1", "타임테이블 조회 성공", timeTableService.getTimeTable(meetingId));
    }

    @GetMapping("/{meetingId}/recommend")
    @Operation(
            summary = "추천 일정 조회",
            description = """
            집계된 시간표 데이터를 기반으로 참여자 중첩률이 높은 순으로 추천 일정을 반환합니다.
            - `availableCount`: 해당 시간대에 참여 가능한 인원 수
            - 상위 N 개 일정만 반환 (서비스 로직에 따라 조정 가능)
            - 인증 불필요 (permitAll)
            """
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @TimeTableErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "추천 일정 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = GET_RECOMMEND_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<List<RecommendedScheduleResponse>> recommend(@PathVariable Integer meetingId) {
        return new ApiResponse<>("200-1", "추천 일정입니다.", timeTableService.recommend(meetingId));
    }
}
