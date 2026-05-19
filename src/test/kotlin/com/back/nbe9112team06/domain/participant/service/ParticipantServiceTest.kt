package com.back.nbe9112team06.domain.participant.service

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.meeting.service.MeetingService
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.participant.dto.request.ParticipantJoinRequest
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.domain.participant.repository.ParticipantRepository
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

@DisplayName("ParticipantService 단위 테스트")
class ParticipantServiceTest {

    private val meetingService: MeetingService = mockk()
    private val participantRepository: ParticipantRepository = mockk()
    private val participantService = ParticipantService(meetingService, participantRepository)

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private fun assertErrorCode(ex: Throwable, expected: ErrorCode) {
        assertThat(ex).isInstanceOf(BusinessException::class.java)
        val be = ex as BusinessException
        assertThat(be.httpStatus).isEqualTo(expected.status)
        assertThat(be.errorCode.code).isEqualTo(expected.code)
        assertThat(be.message).isEqualTo(expected.message)
    }

    private fun buildMeeting(randomUrl: String): Meeting {
        val host = Member("host@test.com", "hash", "호스트", TimezoneType.ASIA_SEOUL)
            .also { ReflectionTestUtils.setField(it, "id", 1) }
        return Meeting("테스트 모임", "STUDY", 60, host, randomUrl)
            .also { ReflectionTestUtils.setField(it, "id", 10) }
    }

    private fun savedParticipant(name: String, password: String, id: Int): Participant =
        Participant(name, password).also { ReflectionTestUtils.setField(it, "id", id) }

    // ── joinMeeting ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("joinMeeting")
    inner class JoinMeeting {

        @Test
        fun `성공 - DB가 부여한 participantId와 guestName을 담은 응답을 반환한다`() {
            val url = "abc123"
            val meeting = buildMeeting(url)
            val saved = savedParticipant("홍길동", "1234", 99)

            every { meetingService.getMeetingByRandomUrlOrThrow(url) } returns meeting
            every { participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234") } returns null
            every { participantRepository.save(any()) } returns saved

            val response = participantService.joinMeeting(url, ParticipantJoinRequest("홍길동", "1234"))

            assertThat(response.participantId).isEqualTo(99)
            assertThat(response.guestName).isEqualTo("홍길동")
        }

        @Test
        fun `실패 - 이름과 비밀번호가 동일한 참가자가 이미 존재하면 DUPLICATE_PARTICIPANT 예외를 던진다`() {
            val url = "abc123"
            val meeting = buildMeeting(url)
            val existing = savedParticipant("홍길동", "1234", 1)

            every { meetingService.getMeetingByRandomUrlOrThrow(url) } returns meeting
            every { participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234") } returns existing

            assertThatThrownBy { participantService.joinMeeting(url, ParticipantJoinRequest("홍길동", "1234")) }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.DUPLICATE_PARTICIPANT) })

            verify(exactly = 0) { participantRepository.save(any()) }
        }

        @Test
        fun `실패 - 존재하지 않는 모임 URL이면 참가자를 저장하지 않고 MEETING_NOT_FOUND 예외를 던진다`() {
            val url = "notExists"
            every { meetingService.getMeetingByRandomUrlOrThrow(url) } throws BusinessException(ErrorCode.MEETING_NOT_FOUND)

            assertThatThrownBy { participantService.joinMeeting(url, ParticipantJoinRequest("홍길동", "1234")) }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.MEETING_NOT_FOUND) })

            verify(exactly = 0) { participantRepository.save(any()) }
        }
    }

    // ── findParticipantOrThrow ────────────────────────────────────────────────

    @Nested
    @DisplayName("findParticipantOrThrow")
    inner class FindParticipantOrThrow {

        @Test
        fun `성공 - 모임·이름·비밀번호가 모두 일치하면 해당 참가자 객체를 반환한다`() {
            val meeting = buildMeeting("url1")
            val expected = savedParticipant("홍길동", "1234", 5)

            every { participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234") } returns expected

            val result = participantService.findParticipantOrThrow(meeting, "홍길동", "1234")

            assertThat(result).isSameAs(expected)
        }

        @Test
        fun `실패 - 이름·비밀번호가 불일치하면 PARTICIPANT_NOT_FOUND 예외를 던진다`() {
            val meeting = buildMeeting("url1")

            every { participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "wrong") } returns null

            assertThatThrownBy { participantService.findParticipantOrThrow(meeting, "홍길동", "wrong") }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.PARTICIPANT_NOT_FOUND) })
        }
    }

    // ── findParticipantByRandomUrlOrThrow ─────────────────────────────────────

    @Nested
    @DisplayName("findParticipantByRandomUrlOrThrow")
    inner class FindParticipantByRandomUrlOrThrow {

        @Test
        fun `성공 - URL·이름·비밀번호 조합이 모두 일치하면 해당 참가자 객체를 반환한다`() {
            val url = "abc123"
            val meeting = buildMeeting(url)
            val expected = savedParticipant("홍길동", "1234", 7)

            every { meetingService.getMeetingByRandomUrlOrThrow(url) } returns meeting
            every { participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "홍길동", "1234") } returns expected

            val result = participantService.findParticipantByRandomUrlOrThrow(url, "홍길동", "1234")

            assertThat(result).isSameAs(expected)
        }

        @Test
        fun `실패 - 존재하지 않는 모임 URL이면 참가자 조회 없이 MEETING_NOT_FOUND 예외를 던진다`() {
            val url = "noSuchUrl"
            every { meetingService.getMeetingByRandomUrlOrThrow(url) } throws BusinessException(ErrorCode.MEETING_NOT_FOUND)

            assertThatThrownBy { participantService.findParticipantByRandomUrlOrThrow(url, "홍길동", "1234") }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.MEETING_NOT_FOUND) })
        }

        @Test
        fun `실패 - 모임은 존재하지만 일치하는 참가 기록이 없으면 PARTICIPANT_NOT_FOUND 예외를 던진다`() {
            val url = "abc123"
            val meeting = buildMeeting(url)

            every { meetingService.getMeetingByRandomUrlOrThrow(url) } returns meeting
            every { participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, "없는사람", "0000") } returns null

            assertThatThrownBy { participantService.findParticipantByRandomUrlOrThrow(url, "없는사람", "0000") }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.PARTICIPANT_NOT_FOUND) })
        }
    }

    // ── deleteParticipant ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteParticipant")
    inner class DeleteParticipant {

        @Test
        fun `성공 - existsById 확인 후 delete를 호출한다`() {
            val participant = savedParticipant("홍길동", "1234", 42)

            every { participantRepository.existsById(42) } returns true
            justRun { participantRepository.delete(participant) }

            participantService.deleteParticipant(participant)

            verify { participantRepository.delete(participant) }
        }

        @Test
        fun `실패 - id가 0인 참가자 삭제 시도 시 delete를 호출하지 않고 PARTICIPANT_NOT_FOUND 예외를 던진다`() {
            val participant = Participant("홍길동", "1234")

            assertThatThrownBy { participantService.deleteParticipant(participant) }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.PARTICIPANT_NOT_FOUND) })

            verify(exactly = 0) { participantRepository.delete(any()) }
        }

        @Test
        fun `실패 - id는 있지만 DB에 존재하지 않는 참가자 삭제 시도 시 PARTICIPANT_NOT_FOUND 예외를 던진다`() {
            val participant = savedParticipant("홍길동", "1234", 99)

            every { participantRepository.existsById(99) } returns false

            assertThatThrownBy { participantService.deleteParticipant(participant) }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex -> assertErrorCode(ex, ErrorCode.PARTICIPANT_NOT_FOUND) })

            verify(exactly = 0) { participantRepository.delete(any()) }
        }
    }
}
