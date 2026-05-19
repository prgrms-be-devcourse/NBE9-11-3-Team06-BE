package com.back.nbe9112team06.domain.meeting.service

import com.back.nbe9112team06.domain.meeting.dto.request.FinalizeRequest
import com.back.nbe9112team06.domain.meeting.dto.request.MeetingCreateRequest
import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus
import com.back.nbe9112team06.domain.meeting.entity.MeetingsDate
import com.back.nbe9112team06.domain.meeting.repository.MeetingRepository
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.member.service.MemberService
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.domain.timetable.service.TimeTableService
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional
import java.time.LocalDateTime
import java.time.LocalTime

class MeetingServiceTest {

    private val meetingRepository = mockk<MeetingRepository>()
    private val memberService = mockk<MemberService>()
    private val timeTableService = mockk<TimeTableService>()
    private val meetingService = MeetingService(meetingRepository, memberService, timeTableService)

    companion object {
        private const val MEETING_ID = 1
        private const val HOST_MEMBER_ID = 10
        private const val OTHER_MEMBER_ID = 99
    }

    // ── 공통 헬퍼 ──────────────────────────────────────────────────────────────

    private fun buildHost(): Member =
        Member("host@test.com", "hash", "모임장", TimezoneType.ASIA_SEOUL)
            .also { ReflectionTestUtils.setField(it, "id", HOST_MEMBER_ID) }

    private fun buildMeeting(status: MeetingStatus): Meeting =
        Meeting("테스트 모임", "STUDY", 60, buildHost(), "testUrl")
            .also {
                ReflectionTestUtils.setField(it, "id", MEETING_ID)
                ReflectionTestUtils.setField(it, "status", status)
            }

    private fun buildMeetingWithParticipants(status: MeetingStatus, count: Int): Meeting =
        buildMeeting(status).apply {
            repeat(count) { i ->
                participants.add(
                    Participant.create("guest$i", "pass$i")
                        .also { ReflectionTestUtils.setField(it, "id", 100 + i) }
                )
            }
        }

    private fun assertErrorCode(ex: BusinessException, expected: ErrorCode) {
        assertThat(ex.httpStatus).isEqualTo(expected.status)
        assertThat(ex.errorCode.code).isEqualTo(expected.code)
        assertThat(ex.message).isEqualTo(expected.message)
    }

    // ── 모임 생성 ──────────────────────────────────────────────────────────────

    @Nested
    inner class `createMeeting` {

        @Test
        fun `성공 - meetingId와 randomUrl 반환`() {
            val host = buildHost()
            every { memberService.findById(HOST_MEMBER_ID) } returns host
            every { meetingRepository.existsByRandomUrl(any()) } returns false

            val savedMeeting = Meeting("새 모임", "STUDY", 60, host, "generatedUrl")
                .also { ReflectionTestUtils.setField(it, "id", MEETING_ID) }
            every { meetingRepository.save(any()) } returns savedMeeting
            justRun { timeTableService.save(any()) }

            val request = MeetingCreateRequest("새 모임", listOf(LocalDate.of(2026, 4, 20)), 60, "STUDY")

            val response = meetingService.createMeeting(HOST_MEMBER_ID, request)

            assertThat(response.meetingId).isEqualTo(MEETING_ID)
            assertThat(response.roomUrl).isEqualTo("generatedUrl")
        }

        @Test
        fun `실패 - 존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외`() {
            every { memberService.findById(HOST_MEMBER_ID) } returns null
            val request = MeetingCreateRequest("새 모임", listOf(LocalDate.of(2026, 4, 20)), 60, "STUDY")

            val ex = assertThrows<BusinessException> { meetingService.createMeeting(HOST_MEMBER_ID, request) }
            assertErrorCode(ex, ErrorCode.MEMBER_NOT_FOUND)
        }
    }

    // ── 랜덤 URL 조회 ──────────────────────────────────────────────────────────

    @Nested
    inner class `getMeetingByRandomUrl` {

        @Test
        fun `성공 - 모임 정보 반환`() {
            val meeting = buildMeeting(MeetingStatus.PENDING).also {
                ReflectionTestUtils.setField(it, "randomUrl", "testUrl123")
                ReflectionTestUtils.setField(it, "createdAt", LocalDateTime.of(2026, 4, 20, 12, 0))
            }
            every { meetingRepository.findByRandomUrl("testUrl123") } returns meeting

            val response = meetingService.getMeetingByRandomUrl("testUrl123")

            assertThat(response.title).isEqualTo("테스트 모임")
            assertThat(response.status).isEqualTo(MeetingStatus.PENDING)
            assertThat(response.roomUrl).isEqualTo("testUrl123")
        }

        @Test
        fun `성공 - 날짜 목록이 오름차순으로 정렬되어 반환된다`() {
            val unsortedDates = mutableListOf(
                MeetingsDate(LocalDate.of(2026, 4, 22), "host"),
                MeetingsDate(LocalDate.of(2026, 4, 20), "host"),
                MeetingsDate(LocalDate.of(2026, 4, 21), "host"),
            )
            val meeting = buildMeeting(MeetingStatus.PENDING).also {
                ReflectionTestUtils.setField(it, "randomUrl", "sortUrl")
                ReflectionTestUtils.setField(it, "createdAt", LocalDateTime.of(2026, 4, 20, 12, 0))
                ReflectionTestUtils.setField(it, "meetingsDates", unsortedDates)
            }
            every { meetingRepository.findByRandomUrl("sortUrl") } returns meeting

            val response = meetingService.getMeetingByRandomUrl("sortUrl")

            assertThat(response.dates).containsExactly(
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 4, 22)
            )
        }

        @Test
        fun `실패 - 존재하지 않는 URL이면 MEETING_NOT_FOUND 예외`() {
            every { meetingRepository.findByRandomUrl("notExists") } returns null

            val ex = assertThrows<BusinessException> { meetingService.getMeetingByRandomUrl("notExists") }
            assertErrorCode(ex, ErrorCode.MEETING_NOT_FOUND)
        }
    }

    // ── 모임 삭제 ──────────────────────────────────────────────────────────────

    @Nested
    inner class `deleteMeeting` {

        @Test
        fun `성공 - 방장이 정상 삭제`() {
            val meeting = buildMeeting(MeetingStatus.PENDING)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)
            justRun { meetingRepository.delete(meeting) }

            meetingService.deleteMeeting(MEETING_ID, HOST_MEMBER_ID)

            verify { meetingRepository.delete(meeting) }
        }

        @Test
        fun `실패 - 방장이 아닌 회원이 삭제 시도 시 NOT_MEETING_HOST 예외`() {
            val meeting = buildMeeting(MeetingStatus.PENDING)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.deleteMeeting(MEETING_ID, OTHER_MEMBER_ID) }
            assertErrorCode(ex, ErrorCode.NOT_MEETING_HOST)
        }
    }

    // ── 호스트 확인 ────────────────────────────────────────────────────────────

    @Nested
    inner class `checkIsHost` {

        @Test
        fun `성공 - 방장이면 true 반환`() {
            val meeting = buildMeeting(MeetingStatus.PENDING).also {
                ReflectionTestUtils.setField(it, "randomUrl", "hostUrl")
            }
            every { meetingRepository.findByRandomUrl("hostUrl") } returns meeting

            assertThat(meetingService.checkIsHost("hostUrl", HOST_MEMBER_ID)).isTrue()
        }

        @Test
        fun `성공 - 방장이 아니면 false 반환`() {
            val meeting = buildMeeting(MeetingStatus.PENDING).also {
                ReflectionTestUtils.setField(it, "randomUrl", "hostUrl")
            }
            every { meetingRepository.findByRandomUrl("hostUrl") } returns meeting

            assertThat(meetingService.checkIsHost("hostUrl", OTHER_MEMBER_ID)).isFalse()
        }
    }

    // ── 일정 확정 ──────────────────────────────────────────────────────────────

    @Nested
    inner class `confirm` {

        private val request = FinalizeRequest(LocalDate.of(2026, 4, 20), LocalTime.of(14, 0))

        @Test
        fun `성공 - 반환값 및 엔티티 상태 변경 검증`() {
            val meeting = buildMeetingWithParticipants(MeetingStatus.PENDING, 1)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val response = meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, request)

            assertThat(response.status).isEqualTo(MeetingStatus.CONFIRMED)
            assertThat(response.date).isEqualTo(LocalDate.of(2026, 4, 20))
            assertThat(response.time).isEqualTo(LocalTime.of(14, 0))
            assertThat(response.message).contains("2026-04-20", "14:00")
            assertThat(meeting.status).isEqualTo(MeetingStatus.CONFIRMED)
        }

        @Test
        fun `실패 - 방장이 아닌 멤버가 확정 시도 시 NOT_MEETING_HOST 예외`() {
            val meeting = buildMeeting(MeetingStatus.PENDING)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.confirm(MEETING_ID, OTHER_MEMBER_ID, request) }
            assertErrorCode(ex, ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `실패 - 참여자가 없는 모임 확정 시도 시 MEETING_NO_PARTICIPANTS 예외`() {
            val meeting = buildMeeting(MeetingStatus.PENDING)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, request) }
            assertErrorCode(ex, ErrorCode.MEETING_NO_PARTICIPANTS)
        }

        @Test
        fun `실패 - 이미 확정된 모임에 재확정 시도 시 ALREADY_CONFIRMED 예외`() {
            val meeting = buildMeetingWithParticipants(MeetingStatus.CONFIRMED, 1)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, request) }
            assertErrorCode(ex, ErrorCode.ALREADY_CONFIRMED)
        }

        @Test
        fun `실패 - 존재하지 않는 모임 확정 시도 시 MEETING_NOT_FOUND 예외`() {
            every { meetingRepository.findById(MEETING_ID) } returns Optional.empty()

            val ex = assertThrows<BusinessException> { meetingService.confirm(MEETING_ID, HOST_MEMBER_ID, request) }
            assertErrorCode(ex, ErrorCode.MEETING_NOT_FOUND)
        }
    }

    // ── 일정 확정 취소 ────────────────────────────────────────────────────────

    @Nested
    inner class `cancelConfirm` {

        @Test
        fun `성공 - CONFIRMED 상태에서 취소 후 PENDING으로 변경`() {
            val meeting = buildMeeting(MeetingStatus.CONFIRMED).also {
                ReflectionTestUtils.setField(it, "confirmedDate", LocalDate.of(2026, 4, 20))
                ReflectionTestUtils.setField(it, "confirmedTime", LocalTime.of(14, 0))
            }
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            meetingService.cancelConfirm(MEETING_ID, HOST_MEMBER_ID)

            assertThat(meeting.status).isEqualTo(MeetingStatus.PENDING)
            assertThat(meeting.confirmedDate).isNull()
            assertThat(meeting.confirmedTime).isNull()
        }

        @Test
        fun `실패 - 방장이 아닌 회원이 취소 시도 시 NOT_MEETING_HOST 예외`() {
            val meeting = buildMeeting(MeetingStatus.CONFIRMED).also {
                ReflectionTestUtils.setField(it, "confirmedDate", LocalDate.of(2026, 4, 20))
                ReflectionTestUtils.setField(it, "confirmedTime", LocalTime.of(14, 0))
            }
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.cancelConfirm(MEETING_ID, OTHER_MEMBER_ID) }
            assertErrorCode(ex, ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `실패 - 미확정 모임 취소 시도 시 NOT_CONFIRMED 예외`() {
            val meeting = buildMeeting(MeetingStatus.PENDING)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.cancelConfirm(MEETING_ID, HOST_MEMBER_ID) }
            assertErrorCode(ex, ErrorCode.NOT_CONFIRMED)
        }
    }

    // ── 확정 일정 조회 ────────────────────────────────────────────────────────

    @Nested
    inner class `getConfirmedSchedule` {

        @Test
        fun `성공 - 확정된 모임이면 정보 반환`() {
            val meeting = buildMeeting(MeetingStatus.CONFIRMED).also {
                ReflectionTestUtils.setField(it, "confirmedDate", LocalDate.of(2026, 4, 20))
                ReflectionTestUtils.setField(it, "confirmedTime", LocalTime.of(14, 0))
            }
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val response = meetingService.getConfirmedSchedule(MEETING_ID)

            assertThat(response.date).isEqualTo(LocalDate.of(2026, 4, 20))
            assertThat(response.time).isEqualTo(LocalTime.of(14, 0))
            assertThat(response.status).isEqualTo(MeetingStatus.CONFIRMED)
        }

        @Test
        fun `실패 - 미확정 모임이면 NOT_CONFIRMED 예외`() {
            val meeting = buildMeeting(MeetingStatus.PENDING)
            every { meetingRepository.findById(MEETING_ID) } returns Optional.of(meeting)

            val ex = assertThrows<BusinessException> { meetingService.getConfirmedSchedule(MEETING_ID) }
            assertErrorCode(ex, ErrorCode.NOT_CONFIRMED)
        }
    }

    // ── 내 모임 목록 조회 ─────────────────────────────────────────────────────

    @Nested
    inner class `getMyMeetings` {

        @Test
        fun `성공 - 본인 모임만 반환`() {
            val host = buildHost()
            val hostMeeting = Meeting("내 모임", "STUDY", 60, host, "url1")
                .also {
                    ReflectionTestUtils.setField(it, "id", MEETING_ID)
                    ReflectionTestUtils.setField(it, "createdAt", LocalDateTime.of(2026, 4, 20, 12, 0))
                }
            every { meetingRepository.findByMember_IdOrderByCreatedAtDesc(HOST_MEMBER_ID) } returns listOf(hostMeeting)

            val result = meetingService.getMyMeetings(HOST_MEMBER_ID)

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo("내 모임")
        }

        @Test
        fun `성공 - 모임이 없으면 빈 목록 반환`() {
            every { meetingRepository.findByMember_IdOrderByCreatedAtDesc(HOST_MEMBER_ID) } returns emptyList()

            val result = meetingService.getMyMeetings(HOST_MEMBER_ID)

            assertThat(result).isEmpty()
        }
    }
}
