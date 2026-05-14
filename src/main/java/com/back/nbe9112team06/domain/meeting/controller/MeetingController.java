package com.back.nbe9112team06.domain.meeting.controller;

import com.back.nbe9112team06.domain.meeting.dto.ConfirmedScheduleResponse;
import com.back.nbe9112team06.domain.meeting.dto.FinalizeRequest;
import com.back.nbe9112team06.domain.meeting.dto.request.MeetingCreateRequest;
import com.back.nbe9112team06.domain.meeting.dto.response.HostCheckResponse;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingCreateResponse;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingEntryResponse;
import com.back.nbe9112team06.domain.meeting.service.MeetingService;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.global.response.ApiResponse;
import com.back.nbe9112team06.global.rq.Rq;
import com.back.nbe9112team06.global.springDoc.annotation.AuthErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.CommonErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.MeetingErrorResponses;
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

import static com.back.nbe9112team06.global.springDoc.example.MeetingApiExamples.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "모임방 생성 API")
public class MeetingController {

    private final MeetingService meetingService;
    private final Rq rq;

    // ── 모임 목록 조회 ──────────────────────────────
    @GetMapping
    @Operation(
            summary = "내 모임 목록 조회",
            description = "로그인한 사용자가 생성한 모든 모임 목록을 조회합니다."
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "모임 목록 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = GET_MY_MEETINGS_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<List<MeetingEntryResponse>> getMyMeetings() {
        Integer memberId = rq.getActor().getId();
        return new ApiResponse<>("200-1", "모임 목록 조회 성공", meetingService.getMyMeetings(memberId));
    }

    // ── 모임 생성 (develop) ──────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "모임방 생성",
            description = """
            새로운 모임방을 생성합니다.
            - `title`: 2~50자, `category`: "PROJECT", "STUDY", "INTERVIEW" 중 선택
            - `dates`: "yyyy-MM-dd" 형식의 날짜 목록 (최소 1개, 오늘 이후만 가능)
            - `duration`: 30~180 분 단위
            - 성공 시 랜덤 10자리 `roomUrl` 반환
            """
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "모임방 생성 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = CREATE_MEETING_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<MeetingCreateResponse> createMeeting(
            @RequestBody @Valid MeetingCreateRequest request
    ) {
        Member actor = rq.getActor();
        Integer memberId = actor.getId();
        MeetingCreateResponse response = meetingService.createMeeting(memberId, request);
        return new ApiResponse<>("201-1", "모임방 생성 성공", response);
    }

    @GetMapping("/{randomUrl}")
    @Operation(
            summary = "랜덤 URL 로 모임방 조회",
            description = "공유된 `randomUrl` 로 모임 상세 정보를 조회합니다. (인증 불필요)"
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "모임방 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = GET_MEETING_BY_URL_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<MeetingEntryResponse> getMeetingByRandomUrl(@PathVariable String randomUrl) {
        MeetingEntryResponse response = meetingService.getMeetingByRandomUrl(randomUrl);
        return new ApiResponse<>("200-1", "모임방 조회 성공", response);
    }

    @GetMapping("/{randomUrl}/check-creator")
    @Operation(
            summary = "해당 방의 방장인지 여부 조회",
            description = "로그인한 사용자가 해당 모임의 생성자(방장)인지 확인합니다."
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(name = "isHost", value = CHECK_CREATOR_IS_HOST_JSON),
                            @ExampleObject(name = "notHost", value = CHECK_CREATOR_NOT_HOST_JSON)
                    }
            )
    )
    public ApiResponse<HostCheckResponse> checkCreator(@PathVariable String randomUrl) {
        Integer memberId = rq.getActor().getId();
        boolean isHost = meetingService.checkIsHost(randomUrl, memberId);

        return new ApiResponse<>(
                "200-1",
                isHost ? "방장이 맞습니다." : "방장이 아닙니다.",
                new HostCheckResponse(isHost)
        );
    }

    // ── 모임 삭제 ──────────────────────────────
    @DeleteMapping("/{meetingId}")
    @Operation(
            summary = "모임 삭제",
            description = "모임방을 삭제합니다. 방장만 실행 가능합니다."
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "모임 삭제 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = DELETE_MEETING_SUCCESS_JSON
                    )
            )
    )
    public void deleteMeeting(@PathVariable Integer meetingId) {
        Integer memberId = rq.getActor().getId();
        meetingService.deleteMeeting(meetingId, memberId);
    }

    // ── 일정 확정/취소/조회 ──────────────────────────────
    @PostMapping("/{meetingId}/confirm")
    @Operation(
            summary = "일정 확정",
            description = """
            모임의 최종 일정을 확정합니다.
            - 방장만 실행 가능
            - `date`: "yyyy-MM-dd", `time`: "HH:mm" 형식
            - 확정 후 참여자들에게 알림 발송 (향후 확장)
            """
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "일정 확정 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = CONFIRM_SCHEDULE_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<ConfirmedScheduleResponse> confirm(
            @PathVariable Integer meetingId,
            @Valid @RequestBody FinalizeRequest request
    ) {
        Integer memberId = rq.getActor().getId();
        return new ApiResponse<>("200-1", "일정이 확정되었습니다.",
                meetingService.confirm(meetingId, memberId, request));
    }

    @DeleteMapping("/{meetingId}/confirm")
    @Operation(
            summary = "일정 확정 취소",
            description = "확정된 모임 일정을 취소하고 `PENDING` 상태로 되돌립니다. 방장만 실행 가능합니다."
    )
    @CommonErrorResponses
    @AuthErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "일정 확정 취소 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = CANCEL_CONFIRM_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<Void> cancelConfirm(
            @PathVariable Integer meetingId
    ) {
        Integer memberId = rq.getActor().getId();
        meetingService.cancelConfirm(meetingId, memberId);
        return new ApiResponse<>("200-1", "일정 확정이 취소되었습니다.", null);
    }

    @GetMapping("/{meetingId}/confirm")
    @Operation(
            summary = "확정된 일정 조회",
            description = "확정된 모임의 최종 일정 정보를 조회합니다. (인증 불필요)"
    )
    @CommonErrorResponses
    @MeetingErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "확정 일정 조회 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value =GET_CONFIRMED_SCHEDULE_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<ConfirmedScheduleResponse> getConfirmedSchedule(
            @PathVariable Integer meetingId
    ) {
        return new ApiResponse<>("200-1", "확정된 일정입니다.", meetingService.getConfirmedSchedule(meetingId));
    }
}