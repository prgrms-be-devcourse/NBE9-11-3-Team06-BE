package com.back.nbe9112team06.domain.meeting.service;

import com.back.nbe9112team06.domain.meeting.dto.response.ConfirmedScheduleResponse;
import com.back.nbe9112team06.domain.meeting.dto.request.FinalizeRequest;
import com.back.nbe9112team06.domain.meeting.dto.request.MeetingCreateRequest;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingCreateResponse;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingEntryResponse;
import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus;
import com.back.nbe9112team06.domain.meeting.repository.MeetingRepository;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.domain.member.service.MemberService;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.domain.timetable.service.TimeTableService;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private TimeTableService timeTableService;

    @InjectMocks
    private MeetingService meetingService;

    private static final Integer MEETING_ID = 1;
    private static final Integer HOST_MEMBER_ID = 10;
    private static final Integer OTHER_MEMBER_ID = 99;

    private Meeting buildMeeting(MeetingStatus status) {
        Member member = new Member("host@test.com", "hash", "모임장", TimezoneType.ASIA_SEOUL);
        ReflectionTestUtils.setField(member, "id", HOST_MEMBER_ID);

        Meeting meeting = new Meeting();
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        ReflectionTestUtils.setField(meeting, "member", member);
        ReflectionTestUtils.setField(meeting, "status", status);
        ReflectionTestUtils.setField(meeting, "title", "테스트 모임");
        ReflectionTestUtils.setField(meeting, "duration", 60);
        ReflectionTestUtils.setField(meeting, "participants", new ArrayList<>());
        ReflectionTestUtils.setField(meeting, "meetingsDates", new ArrayList<>());

        return meeting;
    }

    private Meeting buildMeetingWithParticipants(MeetingStatus status, int participantCount) {
        Meeting meeting = buildMeeting(status);

        for (int i = 0; i < participantCount; i++) {
            Participant participant = Participant.create("guest" + i, "pass" + i);
            ReflectionTestUtils.setField(participant, "id", 100 + i);
            meeting.getParticipants().add(participant);
        }
        return meeting;
    }

    // ── 모임 생성 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createMeeting")
    class CreateMeeting {

        @Test
        @DisplayName("성공 - meetingId와 randomUrl 반환")
        void createMeeting_success() {
            Member member = new Member("host@test.com", "hash", "모임장", TimezoneType.ASIA_SEOUL);
            ReflectionTestUtils.setField(member, "id", HOST_MEMBER_ID);
            given(memberService.findById(HOST_MEMBER_ID)).willReturn(Optional.of(member));
            given(meetingRepository.existsByRandomUrl(any())).willReturn(false);

            Meeting savedMeeting = new Meeting("새 모임", "STUDY", 60, member, "generatedUrl");
            ReflectionTestUtils.setField(savedMeeting, "id", MEETING_ID);
            given(meetingRepository.save(any())).willReturn(savedMeeting);

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "새 모임",
                    List.of(LocalDate.of(2026, 4, 20)),
                    60,
                    "STUDY"
            );

            MeetingCreateResponse response = meetingService.createMeeting(HOST_MEMBER_ID, request);

            assertThat(response.getMeetingId()).isEqualTo(MEETING_ID);
            assertThat(response.getRoomUrl()).isEqualTo("generatedUrl");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원이면 예외 발생")
        void createMeeting_memberNotFound_throwsException() {
            given(memberService.findById(HOST_MEMBER_ID)).willReturn(Optional.empty());

            MeetingCreateRequest request = new MeetingCreateRequest(
                    "새 모임",
                    List.of(LocalDate.of(2026, 4, 20)),
                    60,
                    "STUDY"
            );

            assertThatThrownBy(() -> meetingService.createMeeting(HOST_MEMBER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getCode()));
        }
    }

    // ── 랜덤 URL 조회 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMeetingByRandomUrl")
    class GetMeetingByRandomUrl {

        @Test
        @DisplayName("성공 - 모임 정보 반환")
        void getMeetingByRandomUrl_success() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            ReflectionTestUtils.setField(meeting, "category", "STUDY");
            ReflectionTestUtils.setField(meeting, "randomUrl", "testUrl123");
            ReflectionTestUtils.setField(meeting, "createdAt", LocalDateTime.of(2026, 4, 20, 12, 0));
            given(meetingRepository.findByRandomUrl("testUrl123")).willReturn(meeting);

            MeetingEntryResponse response = meetingService.getMeetingByRandomUrl("testUrl123");

            assertThat(response.getTitle()).isEqualTo("테스트 모임");
            assertThat(response.getStatus()).isEqualTo(MeetingStatus.PENDING);
            assertThat(response.getRoomUrl()).isEqualTo("testUrl123");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 URL이면 예외 발생")
        void getMeetingByRandomUrl_notFound_throwsException() {
            given(meetingRepository.findByRandomUrl("notExists")).willReturn(null);

            assertThatThrownBy(() -> meetingService.getMeetingByRandomUrl("notExists"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MEETING_NOT_FOUND.getCode()));
        }
    }

    // ── 모임 삭제 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteMeeting")
    class DeleteMeeting {

        @Test
        @DisplayName("성공 - 방장이 정상 삭제")
        void deleteMeeting_success() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            meetingService.deleteMeeting(MEETING_ID, HOST_MEMBER_ID);

            verify(meetingRepository).delete(meeting);
        }

        @Test
        @DisplayName("실패 - 방장이 아닌 회원이 삭제 시도 시 예외 발생")
        void deleteMeeting_notHost_throwsException() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() -> meetingService.deleteMeeting(MEETING_ID, OTHER_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_MEETING_HOST.getCode()));
        }
    }

    // ── 일정 확정 ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("성공 - 반환값 및 엔티티 상태 변경 검증")
        void confirm_success() {
            Meeting meeting = buildMeetingWithParticipants(MeetingStatus.PENDING, 1);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            FinalizeRequest request = new FinalizeRequest(LocalDate.of(2026, 4, 20), LocalTime.of(14, 0));
            ConfirmedScheduleResponse response = meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, request);

            assertThat(response.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
            assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 4, 20));
            assertThat(response.getTime()).isEqualTo(LocalTime.of(14, 0));
            assertThat(response.getMessage()).contains("2026-04-20", "14:00");
            assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("실패 - 방장이 아닌 멤버가 확정 시도")
        void confirm_notHost_throwsException() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() ->
                    meetingService.confirm(MEETING_ID, OTHER_MEMBER_ID,
                            new FinalizeRequest(LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_MEETING_HOST.getCode()));
        }

        @Test
        @DisplayName("실패 - 참여자가 없는 모임 확정 시도")
        void confirm_noParticipants_throwsException() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() ->
                    meetingService.confirm(MEETING_ID, HOST_MEMBER_ID,
                            new FinalizeRequest(LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MEETING_NO_PARTICIPANTS.getCode()));
        }

        @Test
        @DisplayName("실패 - 이미 확정된 모임에 재확정 시도")
        void confirm_alreadyConfirmed_throwsException() {
            Meeting meeting = buildMeetingWithParticipants(MeetingStatus.CONFIRMED, 1);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() ->
                    meetingService.confirm(MEETING_ID, HOST_MEMBER_ID,
                            new FinalizeRequest(LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_CONFIRMED.getCode()));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 모임 확정 시도")
        void confirm_meetingNotFound_throwsException() {
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    meetingService.confirm(MEETING_ID, HOST_MEMBER_ID,
                            new FinalizeRequest(LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MEETING_NOT_FOUND.getCode()));
        }
    }

    // ── 일정 확정 취소 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelConfirm")
    class CancelConfirm {

        @Test
        @DisplayName("성공 - CONFIRMED 상태에서 취소 후 PENDING으로 변경")
        void cancelConfirm_success() {
            Meeting meeting = buildMeeting(MeetingStatus.CONFIRMED);
            ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2026, 4, 20));
            ReflectionTestUtils.setField(meeting, "confirmedTime", LocalTime.of(14, 0));
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            meetingService.cancelConfirm(MEETING_ID, HOST_MEMBER_ID);

            assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PENDING);
            assertThat(meeting.getConfirmedDate()).isNull();
            assertThat(meeting.getConfirmedTime()).isNull();
        }

        @Test
        @DisplayName("실패 - 방장이 아닌 회원이 취소 시도 시 예외 발생")
        void cancelConfirm_notHost_throwsException() {
            Meeting meeting = buildMeeting(MeetingStatus.CONFIRMED);
            ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2026, 4, 20));
            ReflectionTestUtils.setField(meeting, "confirmedTime", LocalTime.of(14, 0));
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() -> meetingService.cancelConfirm(MEETING_ID, OTHER_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException ex = (BusinessException) e;
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_MEETING_HOST.getCode());
                        assertThat(ex.getMessage()).isEqualTo("해당 모임의 호스트(방장)만 가능합니다.");
                    });
        }

        @Test
        @DisplayName("실패 - 미확정 모임 취소 시도")
        void cancelConfirm_notConfirmed_throwsException() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() -> meetingService.cancelConfirm(MEETING_ID, HOST_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_CONFIRMED.getCode()));
        }
    }

    // ── 확정 일정 조회 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getConfirmedSchedule")
    class GetConfirmedSchedule {

        @Test
        @DisplayName("성공 - 확정된 모임이면 정보 반환")
        void getConfirmedSchedule_success() {
            Meeting meeting = buildMeeting(MeetingStatus.CONFIRMED);
            ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2026, 4, 20));
            ReflectionTestUtils.setField(meeting, "confirmedTime", LocalTime.of(14, 0));
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            ConfirmedScheduleResponse response = meetingService.getConfirmedSchedule(MEETING_ID);

            assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 4, 20));
            assertThat(response.getTime()).isEqualTo(LocalTime.of(14, 0));
            assertThat(response.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("실패 - 미확정 모임이면 예외 발생")
        void getConfirmedSchedule_notConfirmed_throwsException() {
            Meeting meeting = buildMeeting(MeetingStatus.PENDING);
            given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

            assertThatThrownBy(() -> meetingService.getConfirmedSchedule(MEETING_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_CONFIRMED.getCode()));
        }
    }

    // ── 내 모임 목록 조회 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyMeetings")
    class GetMyMeetings {

        @Test
        @DisplayName("성공 - 본인 모임만 반환")
        void getMyMeetings_onlyReturnsOwnMeetings() {
            Member host = new Member("host@test.com", "hash", "host", TimezoneType.ASIA_SEOUL);
            ReflectionTestUtils.setField(host, "id", HOST_MEMBER_ID);

            Meeting hostMeeting = new Meeting("내 모임", "STUDY", 60, host, "url1");
            ReflectionTestUtils.setField(hostMeeting, "id", MEETING_ID);
            ReflectionTestUtils.setField(hostMeeting, "createdAt", LocalDateTime.of(2026, 4, 20, 12, 0));

            given(meetingRepository.findByMember_IdOrderByCreatedAtDesc(HOST_MEMBER_ID))
                    .willReturn(List.of(hostMeeting));

            List<MeetingEntryResponse> result = meetingService.getMyMeetings(HOST_MEMBER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("내 모임");
        }

        @Test
        @DisplayName("성공 - 모임이 없으면 빈 목록 반환")
        void getMyMeetings_empty() {
            given(meetingRepository.findByMember_IdOrderByCreatedAtDesc(HOST_MEMBER_ID))
                    .willReturn(List.of());

            List<MeetingEntryResponse> result = meetingService.getMyMeetings(HOST_MEMBER_ID);

            assertThat(result).isEmpty();
        }
    }
}