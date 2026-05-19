//package com.back.nbe9112team06.domain.participant.service;
//
//import com.back.nbe9112team06.domain.meeting.entity.Meeting;
//import com.back.nbe9112team06.domain.meeting.service.MeetingService;
//import com.back.nbe9112team06.domain.member.entity.Member;
//import com.back.nbe9112team06.domain.member.entity.TimezoneType;
//import com.back.nbe9112team06.domain.participant.dto.request.ParticipantJoinRequest;
//import com.back.nbe9112team06.domain.participant.dto.response.ParticipantJoinResponse;
//import com.back.nbe9112team06.domain.participant.entity.Participant;
//import com.back.nbe9112team06.domain.participant.repository.ParticipantRepository;
//import com.back.nbe9112team06.global.error.ErrorCode;
//import com.back.nbe9112team06.global.exception.BusinessException;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//import static org.mockito.Mockito.never;
//
//@ExtendWith(MockitoExtension.class)
//class ParticipantServiceTest {
//
//    @Mock
//    private MeetingService meetingService;
//
//    @Mock
//    private ParticipantRepository participantRepository;
//
//    @InjectMocks
//    private ParticipantService participantService;
//
//    // ── 헬퍼 ──────────────────────────────────────────────────────────────
//
//    // TODO: 테스트 Kotlin 변환 후 Java static 메서드 → 최상위 함수 또는 infix fun으로 변경
//    private static void assertErrorCode(Throwable e, ErrorCode errorCode) {
//        assertThat(((BusinessException) e).getErrorCode())
//                .isEqualTo(errorCode.getCode());
//    }
//
//    private Meeting buildMeeting(String randomUrl) {
//        Member host = new Member("host@test.com", "hash", "호스트", TimezoneType.ASIA_SEOUL);
//        ReflectionTestUtils.setField(host, "id", 1);
//        Meeting meeting = new Meeting("테스트 모임", "STUDY", 60, host, randomUrl);
//        ReflectionTestUtils.setField(meeting, "id", 10);
//        return meeting;
//    }
//
//    private Participant savedParticipant(String name, String password, int id) {
//        // TODO: 테스트 Kotlin 변환 후 Participant.create() → Participant() 직접 생성자로 변경
//        Participant p = Participant.create(name, password);
//        ReflectionTestUtils.setField(p, "id", id);
//        return p;
//    }
//
//    // ── joinMeeting ───────────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("joinMeeting")
//    class JoinMeeting {
//
//        @Test
//        @DisplayName("성공 - DB가 부여한 participantId와 guestName을 담은 응답을 반환한다")
//        void joinMeeting_success() {
//            String url = "abc123";
//            Meeting meeting = buildMeeting(url);
//            Participant saved = savedParticipant("홍길동", "1234", 99);
//
//            given(meetingService.getMeetingByRandomUrlOrThrow(url)).willReturn(meeting);
//            given(participantRepository.save(any(Participant.class))).willReturn(saved);
//
//            // TODO: 테스트 Kotlin 변환 후 new ParticipantJoinRequest() → ParticipantJoinRequest() data class 생성자로 변경
//            ParticipantJoinResponse response =
//                    participantService.joinMeeting(url, new ParticipantJoinRequest("홍길동", "1234"));
//
//            // TODO: 테스트 Kotlin 변환 후 getter → 프로퍼티 접근(response.participantId)으로 변경
//            assertThat(response.getParticipantId()).isEqualTo(99);
//            assertThat(response.getGuestName()).isEqualTo("홍길동");
//        }
//
//        @Test
//        @DisplayName("실패 - 존재하지 않는 모임 URL이면 참가자를 저장하지 않고 MEETING_NOT_FOUND 예외를 던진다")
//        void joinMeeting_meetingNotFound() {
//            String url = "notExists";
//            given(meetingService.getMeetingByRandomUrlOrThrow(url))
//                    .willThrow(new BusinessException(ErrorCode.MEETING_NOT_FOUND));
//
//            assertThatThrownBy(() ->
//                    participantService.joinMeeting(url, new ParticipantJoinRequest("홍길동", "1234")))
//                    .isInstanceOf(BusinessException.class)
//                    .satisfies(e -> assertErrorCode(e, ErrorCode.MEETING_NOT_FOUND));
//
//            then(participantRepository).should(never()).save(any());
//        }
//    }
//
//    // ── findParticipantOrThrow ────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("findParticipantOrThrow")
//    class FindParticipantOrThrow {
//
//        @Test
//        @DisplayName("성공 - 모임·이름·비밀번호가 모두 일치하면 해당 참가자 객체를 반환한다")
//        void findParticipantOrThrow_success() {
//            Meeting meeting = buildMeeting("url1");
//            Participant expected = savedParticipant("홍길동", "1234", 5);
//
//            given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234"))
//                    .willReturn(expected);
//
//            Participant result = participantService.findParticipantOrThrow(meeting, "홍길동", "1234");
//
//            assertThat(result).isSameAs(expected);
//        }
//
//        @Test
//        @DisplayName("실패 - 이름·비밀번호가 불일치하면 PARTICIPANT_NOT_FOUND 예외를 던진다")
//        void findParticipantOrThrow_notFound() {
//            Meeting meeting = buildMeeting("url1");
//
//            given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "wrong"))
//                    .willReturn(null);
//
//            assertThatThrownBy(() ->
//                    participantService.findParticipantOrThrow(meeting, "홍길동", "wrong"))
//                    .isInstanceOf(BusinessException.class)
//                    .satisfies(e -> assertErrorCode(e, ErrorCode.PARTICIPANT_NOT_FOUND));
//        }
//    }
//
//    // ── findParticipantByRandomUrlOrThrow ─────────────────────────────────────
//
//    @Nested
//    @DisplayName("findParticipantByRandomUrlOrThrow")
//    class FindParticipantByRandomUrlOrThrow {
//
//        @Test
//        @DisplayName("성공 - URL·이름·비밀번호 조합이 모두 일치하면 해당 참가자 객체를 반환한다")
//        void findParticipantByRandomUrlOrThrow_success() {
//            String url = "abc123";
//            Meeting meeting = buildMeeting(url);
//            Participant expected = savedParticipant("홍길동", "1234", 7);
//
//            given(meetingService.getMeetingByRandomUrlOrThrow(url)).willReturn(meeting);
//            given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234"))
//                    .willReturn(expected);
//
//            Participant result = participantService.findParticipantByRandomUrlOrThrow(url, "홍길동", "1234");
//
//            assertThat(result).isSameAs(expected);
//        }
//
//        @Test
//        @DisplayName("실패 - 존재하지 않는 모임 URL이면 참가자 조회 없이 MEETING_NOT_FOUND 예외를 던진다")
//        void findParticipantByRandomUrlOrThrow_meetingNotFound() {
//            String url = "noSuchUrl";
//            given(meetingService.getMeetingByRandomUrlOrThrow(url))
//                    .willThrow(new BusinessException(ErrorCode.MEETING_NOT_FOUND));
//
//            assertThatThrownBy(() ->
//                    participantService.findParticipantByRandomUrlOrThrow(url, "홍길동", "1234"))
//                    .isInstanceOf(BusinessException.class)
//                    .satisfies(e -> assertErrorCode(e, ErrorCode.MEETING_NOT_FOUND));
//        }
//
//        @Test
//        @DisplayName("실패 - 모임은 존재하지만 일치하는 참가 기록이 없으면 PARTICIPANT_NOT_FOUND 예외를 던진다")
//        void findParticipantByRandomUrlOrThrow_participantNotFound() {
//            String url = "abc123";
//            Meeting meeting = buildMeeting(url);
//
//            given(meetingService.getMeetingByRandomUrlOrThrow(url)).willReturn(meeting);
//            given(participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "없는사람", "0000"))
//                    .willReturn(null);
//
//            assertThatThrownBy(() ->
//                    participantService.findParticipantByRandomUrlOrThrow(url, "없는사람", "0000"))
//                    .isInstanceOf(BusinessException.class)
//                    .satisfies(e -> assertErrorCode(e, ErrorCode.PARTICIPANT_NOT_FOUND));
//        }
//    }
//
//    // ── deleteParticipant ─────────────────────────────────────────────────────
//
//    @Nested
//    @DisplayName("deleteParticipant")
//    class DeleteParticipant {
//
//        @Test
//        @DisplayName("성공 - existsById 확인 후 delete를 호출한다")
//        void deleteParticipant_success() {
//            Participant participant = savedParticipant("홍길동", "1234", 42);
//
//            given(participantRepository.existsById(42)).willReturn(true);
//
//            participantService.deleteParticipant(participant);
//
//            then(participantRepository).should().delete(participant);
//        }
//
//        @Test
//        @DisplayName("실패 - id가 0인 참가자 삭제 시도 시 delete를 호출하지 않고 PARTICIPANT_NOT_FOUND 예외를 던진다")
//        void deleteParticipant_idZero() {
//            // TODO: 테스트 Kotlin 변환 후 Participant.create() → Participant() 직접 생성자로 변경
//            Participant participant = Participant.create("홍길동", "1234");
//
//            assertThatThrownBy(() -> participantService.deleteParticipant(participant))
//                    .isInstanceOf(BusinessException.class)
//                    .satisfies(e -> assertErrorCode(e, ErrorCode.PARTICIPANT_NOT_FOUND));
//
//            then(participantRepository).should(never()).delete(any());
//        }
//
//        @Test
//        @DisplayName("실패 - id는 있지만 DB에 존재하지 않는 참가자 삭제 시도 시 PARTICIPANT_NOT_FOUND 예외를 던진다")
//        void deleteParticipant_notExistsInDb() {
//            Participant participant = savedParticipant("홍길동", "1234", 99);
//
//            given(participantRepository.existsById(99)).willReturn(false);
//
//            assertThatThrownBy(() -> participantService.deleteParticipant(participant))
//                    .isInstanceOf(BusinessException.class)
//                    .satisfies(e -> assertErrorCode(e, ErrorCode.PARTICIPANT_NOT_FOUND));
//
//            then(participantRepository).should(never()).delete(any());
//        }
//    }
//}
