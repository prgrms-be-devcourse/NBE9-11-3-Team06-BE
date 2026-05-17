package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.timeblock.dto.TimeBlockRequest
import com.back.nbe9112team06.domain.timeblock.dto.request.TimeBlockDeleteRequest
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository
import com.back.nbe9112team06.domain.timeblock.service.TimeBlockService
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Sql("/timetable-test-data.sql")
internal class TimeTableServiceTest {
    @Autowired
    lateinit var timeTableService: TimeTableService

    @Autowired
    lateinit var timeBlockService: TimeBlockService

    @Autowired
    lateinit var timeBlockRepository: TimeBlockRepository

    @Test
    fun aggregate_1번방_정확한_결과_검증() {
        // when
        timeTableService.aggregate(1)
        val result = timeTableService.getTimeTable(1)


        // then
        assertThat(result.availableDateTimes).hasSize(1)

        val date = result.availableDateTimes[0]
        assertThat(date.availableDate).isEqualTo("2024-05-20")

        val times = date.availableTimeInfos

        // 시간별 검증 (정렬 가정)
        assertThat(times).hasSize(3)

        // 09:00 → 철수, 영희
        assertThat(times[0].time).isEqualTo("09:00:00")
        assertThat(times[0].participants)
            .containsExactlyInAnyOrder("철수", "영희")

        // 10:00 → 철수
        assertThat(times[1].time).isEqualTo("09:30:00")
        assertThat(times[1].participants)
            .containsExactly("철수")

        // 11:00 → 민수
        assertThat(times[2].time).isEqualTo("11:00:00")
        assertThat(times[2].participants)
            .containsExactly("민수")
    }

    @Test
    fun aggregate_2번방_날짜별_검증() {
        timeTableService.aggregate(2)
        val result = timeTableService.getTimeTable(2)

        assertThat(result.availableDateTimes).hasSize(2)

        val first = result.availableDateTimes[0]
        val second = result.availableDateTimes[1]

        // 날짜 검증
        assertThat(first.availableDate).isEqualTo("2024-05-21")
        assertThat(second.availableDate).isEqualTo("2024-05-22")

        // 시간 + 참가자
        assertThat(first.availableTimeInfos[0].time).isEqualTo("10:00:00")
        assertThat(first.availableTimeInfos[0].participants)
            .containsExactly("지훈")

        assertThat(second.availableTimeInfos[0].participants)
            .containsExactly("수지")
    }

    @Test
    fun aggregate_3번방_완전겹침_검증() {
        timeTableService.aggregate(3)
        val result = timeTableService.getTimeTable(3)

        val date = result.availableDateTimes[0]
        val time = date.availableTimeInfos[0]

        assertThat(time.time).isEqualTo("09:00:00")
        assertThat(time.participants)
            .containsExactlyInAnyOrder("A", "B")
    }

    @Test
    fun 존재하지않는_모임_aggregate_시_예외발생() {
        val exception = catchThrowable {
            timeTableService.aggregate(999)
        } as BusinessException

        assertThat(exception.errorCode)
            .isEqualTo(ErrorCode.MEETING_NOT_FOUND.code)

        assertThat(exception.message)
            .contains("존재하지 않는 모임입니다")
    }

    @Test
    fun 타임블럭없으면_빈_타임테이블반환() {
        // 타임블럭 없는 meetingId
        val meetingId = 4

        // when
        timeTableService.aggregate(meetingId)

        val result = timeTableService.getTimeTable(meetingId)

        // then
        assertThat(result.availableDateTimes)
            .isEmpty()
    }

    @Test
    fun aggregate_여러번호출해도_중복생성되지않음() {
        // when
        timeTableService.aggregate(1)
        timeTableService.aggregate(1)

        val result = timeTableService.getTimeTable(1)

        // then
        assertThat(result.availableDateTimes).hasSize(1)

        val date = result.availableDateTimes[0]
        val times = date.availableTimeInfos

        // 기존과 동일하게 유지되어야 함
        assertThat(times).hasSize(3)

        // 09:00 → 철수, 영희 중복 없어야 함
        assertThat(times[0].participants)
            .containsExactlyInAnyOrder("철수", "영희")

        // participant 수 중복 없어야 함
        assertThat(times[0].participants).hasSize(2)
    }

    @Test
    fun 존재하지않는_모임_getTimeTable_조회시_예외발생() {
        val exception = catchThrowable {
            timeTableService.getTimeTable(666)
        } as BusinessException

        assertThat(exception.errorCode)
            .isEqualTo(ErrorCode.MEETING_NOT_FOUND.code)

        assertThat(exception.message)
            .contains("존재하지 않는 모임입니다")
    }

    @Test
    fun recommend_추천일정_정상조회() {
        // given
        timeTableService.aggregate(1)

        // when
        val result = timeTableService.recommend(1)

        // then
        assertThat(result).isNotEmpty()

        val first = result[0]

        assertThat(first.availableCount).isGreaterThan(0)
        assertThat(first.startTime).isBefore(first.endTime)
    }

    @Test
    fun recommend_참여자많은순_정렬검증() {
        // given
        timeTableService.aggregate(1)

        // when
        val result = timeTableService.recommend(1)

        // then
        for (i in 0 until result.lastIndex) {
            assertThat(result[i].availableCount)
                .isGreaterThanOrEqualTo(
                    result[i + 1].availableCount
                )
        }
    }

    @Test
    fun 타임블럭_등록시_aggregate가_자동반영된다() {

        // given
        val request = TimeBlockRequest(
            guestName = "새참가자",
            guestPassword = "1234",
            availableDateTimes = listOf(
                "2026-05-20 14:00",
                "2026-05-20 14:30",
            ),
        )

        // when
        timeBlockService.registerTimeBlock(5, request)

        val result = timeTableService.getTimeTable(5)

        // then
        assertThat(result.availableDateTimes)
            .hasSize(1)

        val date = result.availableDateTimes[0]

        assertThat(date.availableTimeInfos)
            .hasSize(2)

        assertThat(date.availableTimeInfos[0].participants)
            .containsExactly("새참가자")
    }

    @Test
    fun 참가자한명이_타임블럭삭제시_aggregate가_자동반영된다() {

        // given
        val request = TimeBlockDeleteRequest(
            guestName = "A",
            guestPassword = "1234",
        )

        // when
        timeBlockService.deleteTImeBlock(3, request)

        val result = timeTableService.getTimeTable(3)

        // then
        assertThat(result.availableDateTimes)
            .hasSize(1)

        val participants =
            result.availableDateTimes[0]
                .availableTimeInfos[0]
                .participants

        assertThat(participants)
            .containsExactly("B")
    }

    @Test
    fun 마지막_타임블럭까지_삭제되면_빈_타임테이블이된다() {

        // when
        timeBlockService.deleteTImeBlock(
            3,
            TimeBlockDeleteRequest(
                guestName = "A",
                guestPassword = "1234",
            ),
        )

        timeBlockService.deleteTImeBlock(
            3,
            TimeBlockDeleteRequest(
                guestName = "B",
                guestPassword = "1234",
            ),
        )

        val result = timeTableService.getTimeTable(3)

        // then
        assertThat(result.availableDateTimes)
            .isEmpty()
    }
}
