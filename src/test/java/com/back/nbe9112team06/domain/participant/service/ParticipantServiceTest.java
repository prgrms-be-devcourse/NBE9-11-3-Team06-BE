package com.back.nbe9112team06.domain.participant.service;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.service.MeetingService;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.domain.participant.dto.request.ParticipantJoinRequest;
import com.back.nbe9112team06.domain.participant.dto.response.ParticipantJoinResponse;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.domain.participant.repository.ParticipantRepository;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ParticipantService participantService;

    private Meeting buildMeeting(String randomUrl) {
        Member host = new Member("host@test.com", "hash", "호스트", TimezoneType.ASIA_SEOUL);
        ReflectionTestUtils.setField(host, "id", 1);
        Meeting meeting = Meeting.create("테스트 모임", "STUDY", 60, host, randomUrl);
        ReflectionTestUtils.setField(meeting, "id", 10);
        return meeting;
    }

    private Participant savedParticipant(String name, String password, int id) {
        Participant p = Participant.create(name, password);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    // ── joinMeeting ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 참가 성공 시, DB가 부여한 participantId와 guestName을 담은 응답을 반환한다")
    void joinMeeting_success() {
        String url = "abc123";
        Meeting meeting = buildMeeting(url);
        Participant saved = savedParticipant("홍길동", "1234", 99);

        given(meetingService.getMeetingByRandomUrlOrThrow(url)).willReturn(meeting);
        given(participantRepository.save(any(Participant.class))).willReturn(saved);

        ParticipantJoinResponse response =
                participantService.joinMeeting(url, new ParticipantJoinRequest("홍길동", "1234"));

        assertThat(response.participantId).isEqualTo(99);
        assertThat(response.guestName).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("존재하지 않는 모임 URL로 참가 시도 시, 참가자를 저장하지 않고 MEETING_NOT_FOUND 예외를 던진다")
    void joinMeeting_meetingNotFound() {
        String url = "notExists";
        given(meetingService.getMeetingByRandomUrlOrThrow(url))
                .willThrow(new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        assertThatThrownBy(() ->
                participantService.joinMeeting(url, new ParticipantJoinRequest("홍길동", "1234")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEETING_NOT_FOUND.getCode()));

        then(participantRepository).should(never()).save(any());
    }

    // ── findParticipantOrThrow ────────────────────────────────────────────────

    @Test
    @DisplayName("모임·이름·비밀번호가 모두 일치하는 경우, 해당 참가자 객체를 반환한다")
    void findParticipantOrThrow_success() {
        Meeting meeting = buildMeeting("url1");
        Participant expected = savedParticipant("홍길동", "1234", 5);

        given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234"))
                .willReturn(Optional.of(expected));

        Participant result = participantService.findParticipantOrThrow(meeting, "홍길동", "1234");

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("이름·비밀번호가 불일치하는 경우, PARTICIPANT_NOT_FOUND 예외를 던진다")
    void findParticipantOrThrow_notFound() {
        Meeting meeting = buildMeeting("url1");

        given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "wrong"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                participantService.findParticipantOrThrow(meeting, "홍길동", "wrong"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND.getCode()));
    }

    // ── findParticipantByRandomUrlOrThrow ─────────────────────────────────────

    @Test
    @DisplayName("URL·이름·비밀번호 조합이 모두 일치하는 경우, 해당 참가자 객체를 반환한다")
    void findParticipantByRandomUrlOrThrow_success() {
        String url = "abc123";
        Meeting meeting = buildMeeting(url);
        Participant expected = savedParticipant("홍길동", "1234", 7);

        given(meetingService.getMeetingByRandomUrlOrThrow(url)).willReturn(meeting);
        given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234"))
                .willReturn(Optional.of(expected));

        Participant result = participantService.findParticipantByRandomUrlOrThrow(url, "홍길동", "1234");

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하지 않는 모임 URL로 참가자 조회 시도 시, 참가자 조회 없이 MEETING_NOT_FOUND 예외를 던진다")
    void findParticipantByRandomUrlOrThrow_meetingNotFound() {
        String url = "noSuchUrl";
        given(meetingService.getMeetingByRandomUrlOrThrow(url))
                .willThrow(new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        assertThatThrownBy(() ->
                participantService.findParticipantByRandomUrlOrThrow(url, "홍길동", "1234"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEETING_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("모임은 존재하지만 일치하는 참가 기록이 없는 경우, PARTICIPANT_NOT_FOUND 예외를 던진다")
    void findParticipantByRandomUrlOrThrow_participantNotFound() {
        String url = "abc123";
        Meeting meeting = buildMeeting(url);

        given(meetingService.getMeetingByRandomUrlOrThrow(url)).willReturn(meeting);
        given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "없는사람", "0000"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                participantService.findParticipantByRandomUrlOrThrow(url, "없는사람", "0000"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND.getCode()));
    }

    // ── deleteParticipant ─────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 삭제 시, existsById 확인 후 delete를 호출한다")
    void deleteParticipant_success() {
        Participant participant = savedParticipant("홍길동", "1234", 42);

        given(participantRepository.existsById(42)).willReturn(true);

        participantService.deleteParticipant(participant);

        then(participantRepository).should().delete(participant);
    }

    @Test
    @DisplayName("id가 null인 참가자 객체 삭제 시도 시, delete를 호출하지 않고 PARTICIPANT_NOT_FOUND 예외를 던진다")
    void deleteParticipant_idNull() {
        Participant participant = Participant.create("홍길동", "1234");

        assertThatThrownBy(() -> participantService.deleteParticipant(participant))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND.getCode()));

        then(participantRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("id는 있지만 DB에 존재하지 않는 참가자 삭제 시도 시, delete를 호출하지 않고 PARTICIPANT_NOT_FOUND 예외를 던진다")
    void deleteParticipant_notExistsInDb() {
        Participant participant = savedParticipant("홍길동", "1234", 99);

        given(participantRepository.existsById(99)).willReturn(false);

        assertThatThrownBy(() -> participantService.deleteParticipant(participant))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND.getCode()));

        then(participantRepository).should(never()).delete(any());
    }
}
