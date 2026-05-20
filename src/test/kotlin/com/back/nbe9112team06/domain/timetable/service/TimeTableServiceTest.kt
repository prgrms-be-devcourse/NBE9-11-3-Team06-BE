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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    // ─── aggregate + getTimeTable (Projection 기반이므로 aggregate 불필요) ───

    @Test
    fun `1번방_타임블럭_날짜_시간_참가자가_정확히_조회된다`() {
        // Projection 기반: aggregate 없이 time_block 원본 직접 조회
        val result = timeTableService.getTimeTable(1)

        assertThat(result.availableDateTimes).hasSize(1)

        val date = result.availableDateTimes[0]
        assertThat(date.availableDate).isEqualTo("2024-05-20")

        val times = date.availableTimeInfos
        assertThat(times).hasSize(3)

        assertThat(times[0].time).isEqualTo("09:00:00")
        assertThat(times[0].participants).containsExactlyInAnyOrder("철수", "영희")

        assertThat(times[1].time).isEqualTo("09:30:00")
        assertThat(times[1].participants).containsExactly("철수")

        assertThat(times[2].time).isEqualTo("11:00:00")
        assertThat(times[2].participants).containsExactly("민수")
    }

    @Test
    fun `2번방_날짜별_참가자_조회된다`() {
        val result = timeTableService.getTimeTable(2)

        assertThat(result.availableDateTimes).hasSize(2)

        val first = result.availableDateTimes[0]
        val second = result.availableDateTimes[1]

        assertThat(first.availableDate).isEqualTo("2024-05-21")
        assertThat(second.availableDate).isEqualTo("2024-05-22")

        assertThat(first.availableTimeInfos[0].time).isEqualTo("10:00:00")
        assertThat(first.availableTimeInfos[0].participants).containsExactly("지훈")
        assertThat(second.availableTimeInfos[0].participants).containsExactly("수지")
    }

    @Test
    fun `3번방_같은시간_완전겹침_두참가자_조회된다`() {
        val result = timeTableService.getTimeTable(3)

        val time = result.availableDateTimes[0].availableTimeInfos[0]

        assertThat(time.time).isEqualTo("09:00:00")
        assertThat(time.participants).containsExactlyInAnyOrder("A", "B")
    }

    // ─── aggregate 자체 동작 검증 ───────────────────────────────────────────

    @Test
    fun `aggregate_정상동작_timetable에_집계가_저장된다`() {
        // aggregate는 timetable 테이블에 집계 결과를 저장하는 동작 자체를 검증
        timeTableService.aggregate(1)

        // Projection getTimeTable로 원본 데이터가 여전히 정상 조회되는지 확인
        val result = timeTableService.getTimeTable(1)
        assertThat(result.availableDateTimes).hasSize(1)
        assertThat(result.availableDateTimes[0].availableTimeInfos).hasSize(3)
    }

    @Test
    fun `aggregate_여러번_호출해도_중복_집계되지_않는다`() {
        timeTableService.aggregate(1)
        timeTableService.aggregate(1)

        val result = timeTableService.getTimeTable(1)

        assertThat(result.availableDateTimes).hasSize(1)

        val times = result.availableDateTimes[0].availableTimeInfos
        assertThat(times).hasSize(3)
        assertThat(times[0].participants).containsExactlyInAnyOrder("철수", "영희")
        assertThat(times[0].participants).hasSize(2)
    }

    @Test
    fun `존재하지않는_모임_aggregate_시_예외발생`() {
        val exception = catchThrowable {
            timeTableService.aggregate(999)
        } as BusinessException

        assertThat(exception.errorCode).isEqualTo(ErrorCode.MEETING_NOT_FOUND)
        assertThat(exception.message).contains("존재하지 않는 모임입니다")
    }

    // ─── getTimeTable ────────────────────────────────────────────────────────

    @Test
    fun `타임블럭없는_모임_getTimeTable_빈결과_반환된다`() {
        // meeting 4번은 time_block 데이터 없음
        val result = timeTableService.getTimeTable(4)

        assertThat(result.availableDateTimes).isEmpty()
    }

    @Test
    fun `존재하지않는_모임_getTimeTable_빈결과_반환된다`() {
        // Projection 기반: 슬롯 없으면 예외 대신 빈 결과
        val result = timeTableService.getTimeTable(666)

        assertThat(result.availableDateTimes).isEmpty()
    }

    // ─── recommend ───────────────────────────────────────────────────────────

    @Test
    fun `recommend_aggregate없이_timeblock_원본으로_추천일정_조회된다`() {
        // Projection 기반: aggregate 선행 불필요
        val result = timeTableService.recommend(1)

        assertThat(result).isNotEmpty()
        assertThat(result[0].availableCount).isGreaterThan(0)
        assertThat(result[0].startTime).isBefore(result[0].endTime)
    }

    @Test
    fun `recommend_참여자많은순으로_정렬된다`() {
        val result = timeTableService.recommend(1)

        for (i in 0 until result.lastIndex) {
            assertThat(result[i].availableCount)
                .isGreaterThanOrEqualTo(result[i + 1].availableCount)
        }
    }

    @Test
    fun `recommend_최대_5개까지만_반환된다`() {
        val result = timeTableService.recommend(1)

        assertThat(result.size).isLessThanOrEqualTo(5)
    }

    // ─── TimeBlock 등록/삭제 → getTimeTable 즉시 반영 ────────────────────────

    @Test
    fun `타임블럭_등록시_getTimeTable에_즉시_반영된다`() {
        // '새참가자'는 meeting 5번 participant로 SQL에 존재하지만
        // 아직 타임블럭(가능 시간)은 등록하지 않은 상태

        // ✅ 과거 날짜 검증 통과를 위해 미래 날짜로 동적 생성
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val futureDate = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0)
        val futureDate2 = futureDate.withMinute(30)

        val request = TimeBlockRequest(
            guestName = "새참가자",
            guestPassword = "1234",
            availableDateTimes = listOf(
                futureDate.format(formatter),
                futureDate2.format(formatter),
            ),
        )

        timeBlockService.registerTimeBlock(5, request)
        val result = timeTableService.getTimeTable(5)

        assertThat(result.availableDateTimes).hasSize(1)
        assertThat(result.availableDateTimes[0].availableTimeInfos).hasSize(2)
        assertThat(result.availableDateTimes[0].availableTimeInfos[0].participants)
            .containsExactly("새참가자")
    }

    @Test
    fun `참가자한명_타임블럭삭제시_getTimeTable에_즉시_반영된다`() {
        timeBlockService.deleteTImeBlock(
            3,
            TimeBlockDeleteRequest(guestName = "A", guestPassword = "1234"),
        )
        val result = timeTableService.getTimeTable(3)

        assertThat(result.availableDateTimes).hasSize(1)

        val participants = result.availableDateTimes[0].availableTimeInfos[0].participants
        assertThat(participants).containsExactly("B")
    }

    @Test
    fun `마지막_타임블럭까지_삭제되면_빈_타임테이블이된다`() {
        timeBlockService.deleteTImeBlock(
            3,
            TimeBlockDeleteRequest(guestName = "A", guestPassword = "1234"),
        )
        timeBlockService.deleteTImeBlock(
            3,
            TimeBlockDeleteRequest(guestName = "B", guestPassword = "1234"),
        )

        val result = timeTableService.getTimeTable(3)

        assertThat(result.availableDateTimes).isEmpty()
    }
}