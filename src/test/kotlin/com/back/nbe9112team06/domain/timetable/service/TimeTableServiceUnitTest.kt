package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository
import com.back.nbe9112team06.domain.timetable.repository.TimeTableRepository
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalTime


@ExtendWith(MockKExtension::class)
@DisplayName("TimeTableService 단위 테스트")
class TimeTableServiceUnitTest {

    @MockK
    lateinit var timeTableRepository: TimeTableRepository

    @MockK
    lateinit var timeBlockRepository: TimeBlockRepository

    private val service by lazy {
        TimeTableService(timeTableRepository, timeBlockRepository)
    }

    // ── findConsecutiveTimeGroups 핵심 로직 테스트 ──────────────────────────

    @Test
    fun 연속된_30분_시간은_하나의_그룹으로_묶인다() {
        // given
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0)
        )

        // when
        val result = service.findConsecutiveTimeGroups(times)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0]).hasSize(3)
    }

    @Test
    fun 비연속_시간은_다른_그룹으로_분리된다() {
        // given
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(11, 0)  // 비연속
        )

        // when
        val result = service.findConsecutiveTimeGroups(times)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0]).hasSize(2)
        assertThat(result[1]).hasSize(1)
    }

    @Test
    fun 빈_리스트는_빈그룹을_반환한다() {
        val result = service.findConsecutiveTimeGroups(emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun 단일_시간은_크기1인_그룹_하나를_반환한다() {
        // given
        val times = listOf(LocalTime.of(10, 0))

        // when
        val result = service.findConsecutiveTimeGroups(times)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0]).hasSize(1)
        assertThat(result[0][0]).isEqualTo(LocalTime.of(10, 0))
    }

    @Test
    fun 연속그룹이_여러개_있을때_모두_분리된다() {
        // given: 09:00~09:30, 11:00~11:30, 14:00 각각 분리
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(11, 0),
            LocalTime.of(11, 30),
            LocalTime.of(14, 0)
        )

        // when
        val result = service.findConsecutiveTimeGroups(times)

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0]).hasSize(2)  // 09:00~09:30
        assertThat(result[1]).hasSize(2)  // 11:00~11:30
        assertThat(result[2]).hasSize(1)  // 14:00
    }

    @Test
    fun 각_그룹의_첫번째와_마지막_시간이_정확하다() {
        // given
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            LocalTime.of(13, 0),
            LocalTime.of(13, 30)
        )

        // when
        val result = service.findConsecutiveTimeGroups(times)

        // then
        assertThat(result[0].first()).isEqualTo(LocalTime.of(9, 0))
        assertThat(result[0].last()).isEqualTo(LocalTime.of(10, 0))
        assertThat(result[1].first()).isEqualTo(LocalTime.of(13, 0))
        assertThat(result[1].last()).isEqualTo(LocalTime.of(13, 30))
    }

    @Test
    fun 그룹내_슬라이딩_윈도우_크기2_검증() {
        // given: windowSize=2 (1시간 미팅 가정)
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0)
        )
        val windowSize = 2

        // when
        val groups = service.findConsecutiveTimeGroups(times)
        val windows = mutableListOf<List<LocalTime>>()
        for (group in groups) {
            if (group.size < windowSize) continue
            for (i in 0..group.size - windowSize) {
                windows.add(group.subList(i, i + windowSize))
            }
        }

        // then: (09:00~09:30), (09:30~10:00) 총 2개 윈도우
        assertThat(windows).hasSize(2)
        assertThat(windows[0]).containsExactly(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30)
        )
        assertThat(windows[1]).containsExactly(
            LocalTime.of(9, 30),
            LocalTime.of(10, 0)
        )
    }

    @Test
    fun 그룹크기가_윈도우크기보다_작으면_슬라이딩_불가() {
        // given: windowSize=3, 그룹크기=2
        val times = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(9, 30)
        )
        val windowSize = 3

        // when
        val groups = service.findConsecutiveTimeGroups(times)
        val windows = mutableListOf<List<LocalTime>>()
        for (group in groups) {
            if (group.size < windowSize) continue
            for (i in 0..group.size - windowSize) {
                windows.add(group.subList(i, i + windowSize))
            }
        }

        // then
        assertThat(windows).isEmpty()
    }
}