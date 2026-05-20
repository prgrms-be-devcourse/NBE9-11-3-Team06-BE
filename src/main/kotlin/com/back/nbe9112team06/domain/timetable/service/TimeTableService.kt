package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository
import com.back.nbe9112team06.domain.timetable.dto.DateResponse
import com.back.nbe9112team06.domain.timetable.dto.RecommendedScheduleResponse
import com.back.nbe9112team06.domain.timetable.dto.TimeResponse
import com.back.nbe9112team06.domain.timetable.dto.TimeTableResponse
import com.back.nbe9112team06.domain.timetable.entity.DateInfo
import com.back.nbe9112team06.domain.timetable.entity.TimeInfo
import com.back.nbe9112team06.domain.timetable.entity.TimeTable
import com.back.nbe9112team06.domain.timetable.repository.TimeTableRepository
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class TimeTableService(
    private val timeTableRepository: TimeTableRepository,
    private val timeBlockRepository: TimeBlockRepository
) {
    // 개인 가능일정 통합
    @Transactional
    fun aggregate(meetingId: Int) {

        val timeTable = timeTableRepository.findByMeetingIdForUpdate(meetingId)
            ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

        timeTable.dateInfos.clear()

        // 집계용 전용 Projection 조회
        val slots = timeBlockRepository.findTimeSlotsForAggregation(meetingId)
        if (slots.isEmpty()) {
            timeTableRepository.save(timeTable)
            return
        }
        // slots 를 (date+time) → [participantName] 으로 그룹핑
        val groupedByDate = slots
            .groupBy { it.date }  // Map<LocalDate, List<TimeSlotProjection>>
            .mapValues { (_, slotsByDate) ->
                slotsByDate
                    .groupBy { slot -> LocalDateTime.of(slot.date, slot.time) }
                    .mapValues { (_, entries) ->
                        entries.map { it.participantId }.toSet()
                    }
            }
        for ((date, timeEntries) in groupedByDate) {
            val dateInfo = DateInfo(timeTable, date)
                .also { timeTable.dateInfos.add(it) }
            // 시간 순 정렬 후 TimeInfo 생성
            timeEntries.toSortedMap().forEach { (dateTime, participantIds) ->
                val timeInfo = TimeInfo(dateInfo, dateTime.toLocalTime())
                    .also { dateInfo.timeInfos.add(it) }
                // AdjustResult 생성 (참가자 이름 목록 추가)
                participantIds.forEach { id ->
                    AdjustResult(timeInfo, id)
                        .also { timeInfo.adjustResultList.add(it) }
                }
            }
        }
        timeTableRepository.save(timeTable)
    }

    // meetingId로 TimeTable 검색
    fun findByMeetingId(meetingId: Int): List<TimeTable> {
        return timeTableRepository.findByMeetingId(meetingId)
    }

    // timetable 저장
    fun save(timeTable: TimeTable) {
        timeTableRepository.save(timeTable)
    }

    // 해당 모임의 TimeTable 초기화
    @Transactional
    fun deleteAllByMeetingId(meetingId: Int) {

        val tables = timeTableRepository.findByMeetingId(meetingId)

        timeTableRepository.deleteAll(tables)
    }

    // 미팅 ID로 TimeTable 반환
    @Transactional(readOnly = true)
    fun getTimeTable(meetingId: Int): TimeTableResponse {

        val slots = timeBlockRepository.findScheduleSlotsByMeetingId(meetingId)

        return slots
            .groupBy { it.date }
            .mapValues { (_, slotsByDate) ->
                slotsByDate.groupBy { it.time }
                    .map { (time, entries) ->
                        TimeResponse(time, entries.map { it.participantName }, entries.size)
                    }
                    .sortedBy { it.time }
            }
            .let { timeByDate ->
                TimeTableResponse(
                    timeByDate.entries
                        .sortedBy { it.key }
                        .map { DateResponse(it.key, it.value) }
                )
            }
    }

    @Transactional(readOnly = true)
    fun recommend(meetingId: Int): List<RecommendedScheduleResponse> {
        // 1. 단일 쿼리로 모든 데이터 조회
        val slots = timeBlockRepository.findTimeSlotsWithMeta(meetingId)
        if (slots.isEmpty()) return emptyList()

        // 2. 윈도우 크기 계산
        val meetingDuration = slots.first().meetingDuration
        val windowSize = meetingDuration / 30
        if (windowSize < 1) return emptyList()

        // 3. 데이터 구조화: Map<Date, Map<Time, Set<ParticipantId>>>
        val scheduleData = slots
            .groupBy { it.date }
            .mapValues { (_, dateSlots) ->
                dateSlots
                    .groupBy({ it.time }, { it.participantId })  // ✅ Long 기반
                    .mapValues { (_, ids) -> ids.toSet() }
            }

        val candidates = mutableListOf<RecommendedScheduleResponse>()

        // 4. 날짜별 탐색 + 연속 시간 그룹핑 + 슬라이딩 윈도우
        for ((date, timeSlotsMap) in scheduleData) {
            val sortedTimes = timeSlotsMap.keys.sorted()
            val consecutiveGroups = findConsecutiveTimeGroups(sortedTimes)

            for (group in consecutiveGroups) {
                if (group.size < windowSize) continue

                for (i in 0..group.size - windowSize) {
                    val window = group.subList(i, i + windowSize)

                    // ✅ 교집합으로 전 구간 참여자 계산
                    val availableParticipants = window
                        .mapNotNull { timeSlotsMap[it] }
                        .reduceOrNull { acc, set -> acc intersect set }
                        ?: emptySet()

                    if (availableParticipants.isNotEmpty()) {
                        candidates.add(
                            RecommendedScheduleResponse(
                                date = date,
                                startTime = window.first(),
                                endTime = window.last().plusMinutes(30),
                                availableCount = availableParticipants.size
                            )
                        )
                    }
                }
            }
        }

        // 5. 정렬 및 상위 5개 반환
        return candidates
            .sortedWith(
                compareByDescending<RecommendedScheduleResponse> { it.availableCount }
                    .thenBy { it.date }
                    .thenBy { it.startTime }
            )
            .take(5)
    }

    // 헬퍼 함수: 연속된 시간 그룹 찾기
    internal fun findConsecutiveTimeGroups(sortedTimes: List<LocalTime>): List<List<LocalTime>> {
        if (sortedTimes.isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<LocalTime>>()
        var current = mutableListOf(sortedTimes.first())

        for (i in 1 until sortedTimes.size) {
            val prev = sortedTimes[i - 1]
            val curr = sortedTimes[i]
            if (curr == prev.plusMinutes(30)) {
                current.add(curr)
            } else {
                groups.add(current)
                current = mutableListOf(curr)
            }
        }
        groups.add(current)
        return groups
    }
}