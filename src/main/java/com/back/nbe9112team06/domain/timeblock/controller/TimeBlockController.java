package com.back.nbe9112team06.domain.timeblock.controller;

import com.back.nbe9112team06.domain.timeblock.dto.TimeBlockRequest;
import com.back.nbe9112team06.domain.timeblock.dto.request.TimeBlockDeleteRequest;
import com.back.nbe9112team06.domain.timeblock.dto.response.ParticipantsScheduleResponse;
import com.back.nbe9112team06.domain.timeblock.service.TimeBlockService;
import com.back.nbe9112team06.global.response.ApiResponse;
import com.back.nbe9112team06.global.springDoc.annotation.CommonErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.MeetingErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.TimeBlockErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.back.nbe9112team06.global.springDoc.example.TimeBlockApiExamples.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
@Tag(name = "TimeBlocks", description = "참여자 일정 관리 API")
public class TimeBlockController {

    private final TimeBlockService timeBlockService;

    @PostMapping("/{meetingId}/time-blocks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "참여자 시간표 등록",
            description = """
            특정 미팅에 참여자의 가능한 시간을 등록합니다.
            - `guestName`, `guestPassword` 로 참여자 인증 수행
            - `availableDateTimes`: "yyyy-MM-dd HH:mm" 형식의 30분 단위 시간 목록
            - 과거 시간 또는 중복 시간 등록 불가
            - 중복 등록 시 409 오류 반환
            """
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @TimeBlockErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "시간표 등록 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = ADD_TIMEBLOCK_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<Void> addTimeBlock(@PathVariable Integer meetingId, @RequestBody @Valid TimeBlockRequest timeBlockRequest){
        timeBlockService.registerTimeBlock(meetingId, timeBlockRequest);
        return new ApiResponse<>("201-1","시간표가 등록되었습니다.", null);
    }

    @DeleteMapping("/{meetingId}/time-blocks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "참여자 시간표 삭제",
            description = """
            참여자의 시간표를 삭제합니다.
            - `guestName`, `guestPassword` 로 참여자 인증 수행
            - 시간표 삭제 성공 시 해당 참여자 정보도 함께 삭제됩니다.
            - 삭제된 시간표는 복구할 수 없습니다.
            """
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @TimeBlockErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "시간표 삭제 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = DELETE_TIMEBLOCK_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<Void> deleteTimeBlock(@PathVariable Integer meetingId, @RequestBody @Valid TimeBlockDeleteRequest timeBlockDeleteRequest){
        timeBlockService.deleteTImeBlock(meetingId, timeBlockDeleteRequest);
        return new ApiResponse<>("204-1", "시간표가 삭제되었습니다.", null);
    }

    @GetMapping("/{meetingId}/participants")
    @Operation(
            summary = "참여자 일정 목록 조회",
            description = """
            특정 미팅에 등록된 모든 참여자의 가능한 시간대를 조회합니다.
            - 연속된 30분 단위 시간은 `TimeRangeResponse` 로 그룹화되어 반환됨
            - 예: 14:00, 14:30, 15:00 → {startTime: 14:00, endTime: 15:30}
            - 인증 불필요 (permitAll)
            """
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "참여자 목록 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = GET_PARTICIPANT_SCHEDULES_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<List<ParticipantsScheduleResponse>> getParticipantSchedules(
            @PathVariable Integer meetingId) {
        return new ApiResponse<>("200-1", "참여자 목록입니다.", timeBlockService.getParticipantSchedules(meetingId));
    }
}
