package com.back.nbe9112team06.domain.participant.controller;

import com.back.nbe9112team06.domain.participant.dto.request.ParticipantJoinRequest;
import com.back.nbe9112team06.domain.participant.dto.response.ParticipantJoinResponse;
import com.back.nbe9112team06.domain.participant.service.ParticipantService;
import com.back.nbe9112team06.global.response.ApiResponse;
import com.back.nbe9112team06.global.springDoc.annotation.CommonErrorResponses;
import com.back.nbe9112team06.global.springDoc.annotation.ParticipantErrorResponses;
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

import static com.back.nbe9112team06.global.springDoc.example.ParticipantApiExamples.JOIN_MEETING_SUCCESS_JSON;

@RestController
@RequestMapping("/api/meetings/{randomUrl}/participants")
@RequiredArgsConstructor
@Tag(name = "Participants", description = "비회원 모임방 참가 API")
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "모임방 비회원 참가",
            description = """
            랜덤 URL 로 식별된 모임방에 비회원(게스트)으로 참가합니다.
            - `guestName`: 필수
            - `guestPassword`: 필수 (향후 참가자 인증용으로 활용 가능)
            - 인증 불필요 (permitAll)
            - 중복 이름 참가 허용 (비밀번호가 다르면 별개 참가자로 처리)
            """
    )
    @CommonErrorResponses
    @ParticipantErrorResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "참가 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = JOIN_MEETING_SUCCESS_JSON
                    )
            )
    )
    public ApiResponse<ParticipantJoinResponse> joinMeeting(
            @PathVariable String randomUrl,
            @RequestBody @Valid ParticipantJoinRequest request
    ) {
        ParticipantJoinResponse response = participantService.joinMeeting(randomUrl, request);
        return new ApiResponse<>("201-1", "모임방 참가 성공", response);
    }
}
