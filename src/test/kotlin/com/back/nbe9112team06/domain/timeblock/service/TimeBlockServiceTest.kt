package com.back.nbe9112team06.domain.timeblock.service

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.meeting.service.MeetingService
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.domain.participant.service.ParticipantService
import com.back.nbe9112team06.domain.timeblock.dto.TimeBlockRequest
import com.back.nbe9112team06.domain.timeblock.dto.request.TimeBlockDeleteRequest
import com.back.nbe9112team06.domain.timeblock.entity.AvailableDateTime
import com.back.nbe9112team06.domain.timeblock.entity.AvailableTime
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock
import com.back.nbe9112team06.domain.timeblock.repository.AvailableDateTimeRepository
import com.back.nbe9112team06.domain.timeblock.repository.AvailableTimeRepository
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository
import com.back.nbe9112team06.global.exception.BusinessException
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@ExtendWith(MockKExtension::class)
@DisplayName("TimeBlockService 단위 테스트")
class TimeBlockServiceTest {

    @MockK
    private lateinit var meetingService: MeetingService

    @MockK
    private lateinit var participantService: ParticipantService

    @MockK
    private lateinit var timeBlockRepository: TimeBlockRepository

    @MockK
    private lateinit var availableDateTimeRepository: AvailableDateTimeRepository

    @MockK
    private lateinit var availableTimeRepository: AvailableTimeRepository

    @InjectMockKs
    private lateinit var service: TimeBlockService

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // 과거 검증 통과용 미래 시점 문자열 생성
    private fun futureDateTimeString(plusDays: Long, hour: Int, minute: Int): String =
        LocalDateTime.now()
            .plusDays(plusDays)
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
            .format(formatter)

    private fun newMeeting(id: Int): Meeting =
        Meeting().also { ReflectionTestUtils.setField(it, "id", id) }

    private fun newParticipant(id: Int, name: String, password: String): Participant =
        Participant.create(name, password).also { ReflectionTestUtils.setField(it, "id", id) }

    @Nested
    @DisplayName("toRanges - 연속 30분 구간 묶기")
    inner class ToRanges {

        private val date: LocalDate = LocalDate.of(2026, 4, 20)

        @Test
        fun `빈 리스트면 빈 결과를 반환한다`() {
            val result = service.toRanges(date, emptyList())

            assertThat(result).isEmpty()
        }

        @Test
        fun `단일 슬롯은 30분 범위 하나로 만든다`() {
            val result = service.toRanges(date, listOf(LocalTime.of(14, 0)))

            assertThat(result).hasSize(1)
            assertThat(result[0].date).isEqualTo(date)
            assertThat(result[0].startTime).isEqualTo(LocalTime.of(14, 0))
            assertThat(result[0].endTime).isEqualTo(LocalTime.of(14, 30))
        }

        @Test
        fun `연속된 슬롯 3개는 하나의 범위로 병합된다`() {
            val slots = listOf(
                LocalTime.of(14, 0),
                LocalTime.of(14, 30),
                LocalTime.of(15, 0),
            )

            val result = service.toRanges(date, slots)

            assertThat(result).hasSize(1)
            assertThat(result[0].startTime).isEqualTo(LocalTime.of(14, 0))
            assertThat(result[0].endTime).isEqualTo(LocalTime.of(15, 30))
        }

        @Test
        fun `끊긴 슬롯은 여러 범위로 분리된다`() {
            val slots = listOf(
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                // 10:00 빠짐
                LocalTime.of(14, 0),
                LocalTime.of(14, 30),
                LocalTime.of(15, 0),
            )

            val result = service.toRanges(date, slots)

            assertThat(result).hasSize(2)
            assertThat(result[0].startTime).isEqualTo(LocalTime.of(9, 0))
            assertThat(result[0].endTime).isEqualTo(LocalTime.of(10, 0))
            assertThat(result[1].startTime).isEqualTo(LocalTime.of(14, 0))
            assertThat(result[1].endTime).isEqualTo(LocalTime.of(15, 30))
        }
    }

    @Nested
    @DisplayName("validateAvailableDateTime - 입력 검증")
    inner class ValidateAvailableDateTime {

        @Test
        fun `중복된 시간이 들어오면 예외가 발생한다`() {
            val dt = futureDateTimeString(1, 14, 0)

            assertThatThrownBy { service.validateAvailableDateTime(listOf(dt, dt)) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessageContaining("시간 선택이 중복")
        }

        @Test
        fun `날짜 형식이 잘못되면 예외가 발생한다`() {
            assertThatThrownBy {
                service.validateAvailableDateTime(listOf("2026/04/20 14:00"))
            }
                .isInstanceOf(BusinessException::class.java)
                .hasMessageContaining("올바른 날짜 형식")
        }

        @Test
        fun `과거 시간이 들어오면 예외가 발생한다`() {
            val past = LocalDateTime.now()
                .minusDays(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .format(formatter)

            assertThatThrownBy { service.validateAvailableDateTime(listOf(past)) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessageContaining("과거")
        }

        @Test
        fun `30분 단위가 아니면 예외가 발생한다`() {
            val dt = futureDateTimeString(1, 14, 15) // 14:15

            assertThatThrownBy { service.validateAvailableDateTime(listOf(dt)) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessageContaining("30분 단위")
        }

        @Test
        fun `정상 입력이면 예외가 발생하지 않는다`() {
            val dt1 = futureDateTimeString(1, 14, 0)
            val dt2 = futureDateTimeString(1, 14, 30)

            service.validateAvailableDateTime(listOf(dt1, dt2))
        }
    }

    @Nested
    @DisplayName("buildDateTimeMap - 날짜별 시간 그룹핑")
    inner class BuildDateTimeMap {

        @Test
        fun `같은 날짜의 시간들은 하나의 키로 묶인다`() {
            val day1a = futureDateTimeString(1, 14, 0)
            val day1b = futureDateTimeString(1, 14, 30)
            val day2 = futureDateTimeString(2, 10, 0)

            val result = service.buildDateTimeMap(listOf(day1a, day1b, day2))

            val day1Date = LocalDateTime.parse(day1a, formatter).toLocalDate()
            val day2Date = LocalDateTime.parse(day2, formatter).toLocalDate()

            assertThat(result).hasSize(2)
            assertThat(result[day1Date]).containsExactlyInAnyOrder(
                LocalTime.of(14, 0),
                LocalTime.of(14, 30),
            )
            assertThat(result[day2Date]).containsExactly(LocalTime.of(10, 0))
        }
    }

    @Nested
    @DisplayName("registerTimeBlock - 등록 흐름")
    inner class RegisterTimeBlock {

        @Test
        fun `이미 등록된 TimeBlock이 있으면 예외가 발생하고 저장은 호출되지 않는다`() {
            val meeting = newMeeting(1)
            val participant = newParticipant(10, "홍길동", "pw")
            val existing = TimeBlock.create(meeting, participant)

            every { meetingService.getMeetingOrThrow(1) } returns meeting
            every { participantService.findParticipantOrThrow(meeting, "홍길동", "pw") } returns participant
            every { timeBlockRepository.findByMeetingAndParticipant(meeting, participant) } returns existing

            val req = TimeBlockRequest(
                guestName = "홍길동",
                guestPassword = "pw",
                availableDateTimes = listOf(futureDateTimeString(1, 14, 0)),
            )

            assertThatThrownBy { service.registerTimeBlock(1, req) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessageContaining("시간표가 이미 등록")

            verify(exactly = 0) { timeBlockRepository.save(any()) }
            verify(exactly = 0) { availableDateTimeRepository.save(any()) }
            verify(exactly = 0) { availableTimeRepository.save(any()) }
        }

        @Test
        fun `정상 등록이면 TimeBlock 1회, AvailableDateTime은 날짜 수만큼, AvailableTime은 슬롯 수만큼 저장된다`() {
            val meeting = newMeeting(2)
            val participant = newParticipant(20, "김철수", "1234")

            every { meetingService.getMeetingOrThrow(2) } returns meeting
            every { participantService.findParticipantOrThrow(meeting, "김철수", "1234") } returns participant
            every { timeBlockRepository.findByMeetingAndParticipant(meeting, participant) } returns null
            every { timeBlockRepository.save(any()) } answers { firstArg() }
            every { availableDateTimeRepository.save(any()) } answers { firstArg() }
            every { availableTimeRepository.save(any()) } answers { firstArg() }

            val req = TimeBlockRequest(
                guestName = "김철수",
                guestPassword = "1234",
                availableDateTimes = listOf(
                    futureDateTimeString(1, 14, 0),
                    futureDateTimeString(1, 14, 30),
                    futureDateTimeString(2, 10, 0),
                ),
            )

            service.registerTimeBlock(2, req)

            verify(exactly = 1) { timeBlockRepository.save(any<TimeBlock>()) }
            verify(exactly = 2) { availableDateTimeRepository.save(any<AvailableDateTime>()) } // 날짜 2종
            verify(exactly = 3) { availableTimeRepository.save(any<AvailableTime>()) } // 슬롯 3개
        }
    }

    @Nested
    @DisplayName("deleteTImeBlock - 삭제 흐름")
    inner class DeleteTimeBlock {

        @Test
        fun `삭제할 TimeBlock이 없으면 예외가 발생하고 삭제는 호출되지 않는다`() {
            val meeting = newMeeting(3)
            val participant = newParticipant(30, "박영희", "pw2")

            every { meetingService.getMeetingOrThrow(3) } returns meeting
            every { participantService.findParticipantOrThrow(meeting, "박영희", "pw2") } returns participant
            every { timeBlockRepository.findByMeetingAndParticipant(meeting, participant) } returns null

            val req = TimeBlockDeleteRequest(guestName = "박영희", guestPassword = "pw2")

            assertThatThrownBy { service.deleteTImeBlock(3, req) }
                .isInstanceOf(BusinessException::class.java)
                .hasMessageContaining("삭제할 시간이 없습니다")

            verify(exactly = 0) { timeBlockRepository.delete(any()) }
            verify(exactly = 0) { participantService.deleteParticipant(any()) }
        }

        @Test
        fun `정상 삭제면 TimeBlock과 Participant가 순서대로 삭제된다`() {
            val meeting = newMeeting(4)
            val participant = newParticipant(40, "이몽룡", "pw3")
            val existing = TimeBlock.create(meeting, participant)

            every { meetingService.getMeetingOrThrow(4) } returns meeting
            every { participantService.findParticipantOrThrow(meeting, "이몽룡", "pw3") } returns participant
            every { timeBlockRepository.findByMeetingAndParticipant(meeting, participant) } returns existing
            every { timeBlockRepository.delete(existing) } returns Unit
            every { participantService.deleteParticipant(participant) } returns Unit

            val req = TimeBlockDeleteRequest(guestName = "이몽룡", guestPassword = "pw3")

            service.deleteTImeBlock(4, req)

            verify(exactly = 1) { timeBlockRepository.delete(existing) }
            verify(exactly = 1) { participantService.deleteParticipant(participant) }
        }
    }
}