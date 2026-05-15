package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.timetable.dto.TimeResponse
import com.back.nbe9112team06.global.exception.BusinessException
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
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
internal class TimeTableServiceTestKotlin {
    @Autowired
    lateinit var timeTableService: TimeTableService


    @Test
    fun aggregate_1번방_정확한_결과_검증() {
        // when

        timeTableService.aggregate(1)
        val result = timeTableService.getTimeTable(1)


        // then
        Assertions.assertThat(result.availableDateTimes).hasSize(1)

        val date = result.availableDateTimes[0]
        Assertions.assertThat(date.availableDate).isEqualTo("2024-05-20")

        val times: List<TimeResponse> = date.availableTimeInfos

        // 시간별 검증 (정렬 가정)
        Assertions.assertThat(times).hasSize(3)

        // 09:00 → 철수, 영희
        Assertions.assertThat(times[0].time).isEqualTo("09:00:00")
        Assertions.assertThat(times[0].participants)
            .containsExactlyInAnyOrder("철수", "영희")

        // 10:00 → 철수
        Assertions.assertThat(times[1].time).isEqualTo("10:00:00")
        Assertions.assertThat(times[1].participants)
            .containsExactly("철수")

        // 11:00 → 민수
        Assertions.assertThat(times[2].time).isEqualTo("11:00:00")
        Assertions.assertThat(times[2].participants)
            .containsExactly("민수")
    }

    @Test
    fun aggregate_2번방_날짜별_검증() {
        timeTableService.aggregate(2)
        val result = timeTableService.getTimeTable(2)

        Assertions.assertThat(result.availableDateTimes).hasSize(2)

        val first = result.availableDateTimes[0]
        val second = result.availableDateTimes[1]

        // 날짜 검증
        Assertions.assertThat(first.availableDate).isEqualTo("2024-05-21")
        Assertions.assertThat(second.availableDate).isEqualTo("2024-05-22")

        // 시간 + 참가자
        Assertions.assertThat(first.availableTimeInfos[0].time).isEqualTo("10:00:00")
        Assertions.assertThat(first.availableTimeInfos[0].participants)
            .containsExactly("지훈")

        Assertions.assertThat(second.availableTimeInfos[0].participants)
            .containsExactly("수지")
    }

    @Test
    fun aggregate_3번방_완전겹침_검증() {
        timeTableService.aggregate(3)
        val result = timeTableService.getTimeTable(3)

        val date = result.availableDateTimes[0]
        val time = date.availableTimeInfos[0]

        Assertions.assertThat(time.time).isEqualTo("09:00:00")
        Assertions.assertThat(time.participants)
            .containsExactlyInAnyOrder("A", "B")
    }

    @Test
    fun 존재하지않는_모임_aggregate_시_예외발생() {
        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { timeTableService.aggregate(999) })
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("존재하지 않는 모임입니다") // 메시지 맞게 조정
    }

    @Test
    fun 타임블럭없으면_aggregate_시_예외발생() {
        // 타임블럭 없는 meetingId

        val meetingId = 10

        Assertions.assertThatThrownBy(ThrowableAssert.ThrowingCallable { timeTableService.aggregate(meetingId) })
            .isInstanceOf(BusinessException::class.java)
    }
}