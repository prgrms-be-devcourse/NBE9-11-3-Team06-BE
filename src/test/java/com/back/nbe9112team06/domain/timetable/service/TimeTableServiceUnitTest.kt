package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository
import com.back.nbe9112team06.domain.timetable.entity.DateInfo
import com.back.nbe9112team06.domain.timetable.entity.TimeInfo
import com.back.nbe9112team06.domain.timetable.repository.TimeTableRepository
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockKExtension::class)
@DisplayName("TimeTableService 단위 테스트")
class TimeTableServiceUnitTest {

    @io.mockk.impl.annotations.MockK
    lateinit var timeTableRepository: TimeTableRepository

    @io.mockk.impl.annotations.MockK
    lateinit var timeBlockRepository: TimeBlockRepository

    private val service by lazy {
        TimeTableService(
            timeTableRepository,
            timeBlockRepository
        )
    }

    @Test
    fun 연속된_30분_시간은_하나의_그룹으로_묶인다() {

        // given
        val slots = listOf(
            createTimeInfo("09:00"),
            createTimeInfo("09:30"),
            createTimeInfo("10:00")
        )

        // when
        val result = service.groupConsecutiveSlots(slots)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0]).hasSize(3)
    }

    @Test
    fun 비연속_시간은_다른_그룹으로_분리된다() {

        // given
        val slots = listOf(
            createTimeInfo("09:00"),
            createTimeInfo("09:30"),
            createTimeInfo("11:00")
        )

        // when
        val result = service.groupConsecutiveSlots(slots)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0]).hasSize(2)
        assertThat(result[1]).hasSize(1)
    }

    @Test
    fun 빈_리스트는_빈그룹을_반환한다() {

        // when
        val result = service.groupConsecutiveSlots(emptyList())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun recommend용_연속슬롯_1시간_추천검증() {

        // given
        val dateInfo = createDateInfo(
            listOf(
                createTimeInfo("09:00", 2),
                createTimeInfo("09:30", 2)
            )
        )

        // when
        val groups = service.groupConsecutiveSlots(
            dateInfo.timeInfos.sortedBy { it.time }
        )

        // then
        assertThat(groups).hasSize(1)
        assertThat(groups[0]).hasSize(2)

        val first = groups[0].first()
        val last = groups[0].last()

        assertThat(first.time).isEqualTo(LocalTime.parse("09:00"))
        assertThat(last.time).isEqualTo(LocalTime.parse("09:30"))

        val minCount = groups[0]
            .minOf { it.adjustResultList.size }

        assertThat(minCount).isEqualTo(2)
    }

    private fun createDateInfo(
        timeInfos: List<TimeInfo>
    ): DateInfo {

        val dateInfo = DateInfo(
            timeTable = mockk(),
            date = LocalDate.parse("2024-05-20")
        )

        dateInfo.timeInfos.addAll(timeInfos)

        return dateInfo
    }

    private fun createTimeInfo(
        time: String,
        participantCount: Int = 0
    ): TimeInfo {

        val timeInfo = TimeInfo(
            dateInfo = mockk(),
            time = LocalTime.parse(time)
        )

        repeat(participantCount) {

            timeInfo.adjustResultList.add(
                AdjustResult(
                    timeInfo,
                    "user$it"
                )
            )
        }

        return timeInfo
    }
}
