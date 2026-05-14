package com.back.nbe9112team06.domain.meeting.service;

import com.back.nbe9112team06.domain.meeting.dto.ConfirmedScheduleResponse;
import com.back.nbe9112team06.domain.meeting.dto.FinalizeRequest;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingEntryResponse;
import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus;
import com.back.nbe9112team06.domain.meeting.repository.MeetingRepository;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

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

    @Test
    @DisplayName("정상 확정 - 반환값 및 엔티티 상태 변경 검증")
    void confirm_success() {
        Meeting meeting = buildMeetingWithParticipants(MeetingStatus.PENDING, 1);
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        FinalizeRequest request = new FinalizeRequest(
                LocalDate.of(2026, 4, 20),
                LocalTime.of(14, 0)
        );

        ConfirmedScheduleResponse response = meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, request);

        assertThat(response.status()).isEqualTo(MeetingStatus.CONFIRMED);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(response.time()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.message()).contains("2026-04-20", "14:00");
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("권한 없음 - 모임장이 아닌 멤버가 확정 시도")
    void confirm_notHost_throwsException() {
        Meeting meeting = buildMeeting(MeetingStatus.PENDING);
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        assertThatThrownBy(() ->
                meetingService.confirm(MEETING_ID, OTHER_MEMBER_ID, new FinalizeRequest(
                        LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_MEETING_HOST.getCode()));
    }

    @Test
    @DisplayName("중복 확정 - 이미 확정된 모임에 재확정 시도")
    void confirm_alreadyConfirmed_throwsException() {
        Meeting meeting = buildMeetingWithParticipants(MeetingStatus.CONFIRMED, 1);
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        assertThatThrownBy(() ->
                meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, new FinalizeRequest(
                        LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CONFIRMED.getCode()));
    }

    @Test
    @DisplayName("정상 취소 - CONFIRMED 상태에서 취소 후 PENDING으로 변경")
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
    @DisplayName("존재하지 않는 모임 확정 시도")
    void confirm_meetingNotFound_throwsException() {
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, new FinalizeRequest(
                        LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEETING_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("잘못된 입력 - 미확정 모임 취소 시도")
    void cancelConfirm_notConfirmed_throwsException() {
        Meeting meeting = buildMeeting(MeetingStatus.PENDING);
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        assertThatThrownBy(() ->
                meetingService.cancelConfirm(MEETING_ID, HOST_MEMBER_ID)
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_CONFIRMED.getCode()));
    }

    @Test
    @DisplayName("모임 삭제 - 방장이 아닌 회원이 삭제 시도 시 예외 발생")
    void deleteMeeting_notHost_throwsException() {
        Meeting meeting = buildMeeting(MeetingStatus.PENDING);
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        assertThatThrownBy(() ->
                meetingService.deleteMeeting(MEETING_ID, OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e ->
                        assertThat(((BusinessException) e).getErrorCode())
                                .isEqualTo("MEETING-002"));
    }

    @Test
    @DisplayName("일정 확정 취소 - 방장이 아닌 회원이 취소 시도 시 예외 발생")
    void cancelConfirm_notHost_throwsException() {
        Meeting meeting = buildMeeting(MeetingStatus.CONFIRMED);
        ReflectionTestUtils.setField(meeting, "confirmedDate", LocalDate.of(2026, 4, 20));
        ReflectionTestUtils.setField(meeting, "confirmedTime", LocalTime.of(14, 0));
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        assertThatThrownBy(() ->
                meetingService.cancelConfirm(MEETING_ID, OTHER_MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException ex = (BusinessException) e;
                    assertThat(ex.getErrorCode()).isEqualTo("MEETING-002");
                    assertThat(ex.getMessage()).isEqualTo("해당 모임의 호스트(방장)만 가능합니다.");
                });
    }

    @Test
    @DisplayName("내 모임 목록 조회 - 다른 회원의 모임은 목록에 포함되지 않음")
    void getMyMeetings_onlyReturnsOwnMeetings() {
        Member host = new Member("host@test.com", "hash", "host", TimezoneType.ASIA_SEOUL);
        ReflectionTestUtils.setField(host, "id", HOST_MEMBER_ID);

        Member other = new Member("other@test.com", "hash", "other", TimezoneType.ASIA_SEOUL);
        ReflectionTestUtils.setField(other, "id", OTHER_MEMBER_ID);

        Meeting hostMeeting = Meeting.create("내 모임", "STUDY", 60, host, "url1");
        Meeting otherMeeting = Meeting.create("다른 사람 모임", "STUDY", 60, other, "url2");

        given(meetingRepository.findByMember_IdOrderByCreatedAtDesc(HOST_MEMBER_ID))
                .willReturn(List.of(hostMeeting));

        List<MeetingEntryResponse> result = meetingService.getMyMeetings(HOST_MEMBER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("내 모임");
        assertThat(result.get(0).title()).isNotEqualTo("다른 사람 모임");
    }

    @Test
    @DisplayName("참여자 없는 모임 확정 시도 - MEETING-005 발생")
    void confirm_noParticipants_throwsException() {
        Meeting meeting = buildMeeting(MeetingStatus.PENDING);
        given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));

        assertThatThrownBy(() ->
                meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, new FinalizeRequest(
                        LocalDate.of(2026, 4, 20), LocalTime.of(14, 0)))
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEETING_NO_PARTICIPANTS.getCode())); // MEETING-005
    }
}