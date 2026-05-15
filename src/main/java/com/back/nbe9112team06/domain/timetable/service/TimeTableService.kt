package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현재 타 도메인의 JpaRepository 메서드를 그대로 사용 중
 * merge 이후 각 Service의 메서드로 수정 필요
 * meeting 생성시 빈 timetable 생성되도록 하기
 */
@Service
class TimeTableService(
    private val timeTableRepository: TimeTableRepository,
    private val timeBlockRepository: TimeBlockRepository
) {

    // 개인 가능일정 통합
    @Transactional
    fun aggregate(meetingId: Int) {

        val timeTable = timeTableRepository.findByMeetingIdForUpdate(meetingId)
            .orElseThrow { BusinessException(ErrorCode.MEETING_NOT_FOUND) }

        timeTable.dateInfos.clear()

        // key: 날짜+시간, value: 참가자 이름 리스트
        val timeToParticipantsNames = mutableMapOf<LocalDateTime, MutableList<String>>()

        val timeBlocks = findWithAll(meetingId)

        for (timeBlock in timeBlocks) {

            val participantName = timeBlock.participant.guestName

            for (availableDateTime in timeBlock.availableDateTimes) {

                val date = availableDateTime.date

                for (availableTime in availableDateTime.availableTimes) {

                    val key = LocalDateTime.of(date, availableTime.time)

                    timeToParticipantsNames
                        .computeIfAbsent(key) { mutableListOf() }
                        .add(participantName)
                }
            }
        }

        // key: 날짜, value: 해당 날짜의 시간 엔트리
        val dateMap =
            mutableMapOf<LocalDate, MutableList<Map.Entry<LocalDateTime, MutableList<String>>>>()

        for (entry in timeToParticipantsNames.entries) {

            val date = entry.key.toLocalDate()

            dateMap
                .computeIfAbsent(date) { mutableListOf() }
                .add(entry)
        }

        for ((date, timeEntries) in dateMap) {

            val dateInfo = DateInfo(timeTable, date)
            timeTable.dateInfos.add(dateInfo)

            for ((dateTime, participantNames) in timeEntries) {

                val timeInfo = TimeInfo(dateInfo, dateTime.toLocalTime())
                dateInfo.timeInfos.add(timeInfo)

                for (participantName in participantNames) {

                    val adjustResult = AdjustResult(timeInfo, participantName)
                    timeInfo.adjustResultList.add(adjustResult)
                }
            }
        }

        timeTableRepository.save(timeTable)
    }

    // meetingId로 TimeTable 검색
    fun findByMeetingId(meetingId: Int): List<TimeTable> {
        return timeTableRepository.findByMeeting_Id(meetingId)
    }

    // timetable 저장
    fun save(timeTable: TimeTable) {
        timeTableRepository.save(timeTable)
    }

    // 해당 모임의 TimeTable 초기화
    @Transactional
    fun deleteAllByMeetingId(meetingId: Int) {

        val tables = timeTableRepository.findByMeeting_Id(meetingId)

        timeTableRepository.deleteAll(tables)
    }

    // 타임블록 DB 에서 데이터 꺼내기
    fun findWithAll(meetingId: Int): List<TimeBlock> {

        val timeBlocks = timeBlockRepository.findWithAll(meetingId)

        if (timeBlocks.isEmpty()) {
            throw BusinessException(ErrorCode.NOT_FOUND)
        }

        return timeBlocks
    }

    // 미팅 ID로 TimeTable 반환
    @Transactional
    fun getTimeTable(meetingId: Int): TimeTableResponse {

        val tables = timeTableRepository.findByMeeting_Id(meetingId)

        if (tables.isEmpty()) {
            return TimeTableResponse(emptyList())
        }

        val table = tables[0]

        val dateResponses = mutableListOf<DateResponse>()

        for (dateInfo in table.dateInfos) {

            val timeResponses = mutableListOf<TimeResponse>()

            for (timeInfo in dateInfo.timeInfos) {

                val participants = timeInfo.adjustResultList
                    .map { it.name }

                timeResponses.add(
                    TimeResponse(
                        timeInfo.time,
                        participants as List<String>,
                        participants.size
                    )
                )
            }

            timeResponses.sortBy { it.time }

            dateResponses.add(
                DateResponse(
                    dateInfo.date,
                    timeResponses
                )
            )
        }

        dateResponses.sortBy { it.availableDate }

        return TimeTableResponse(dateResponses)
    }

    @Transactional
    fun recommend(meetingId: Int): List<RecommendedScheduleResponse> {

        val tables = timeTableRepository.findByMeeting_Id(meetingId)

        if (tables.isEmpty()) {
            return emptyList()
        }

        val table = tables[0]

        // 회의 시간(분) -> 슬롯 수
        val windowSize = table.meeting.duration / 30

        if (windowSize < 1) {
            return emptyList()
        }

        val candidates = mutableListOf<RecommendedScheduleResponse>()

        for (dateInfo in table.dateInfos) {

            val slots = dateInfo.timeInfos
                .sortedBy { it.time }

            for (group in groupConsecutiveSlots(slots)) {

                if (group.size < windowSize) continue

                for (i in 0..group.size - windowSize) {

                    val window = group.subList(i, i + windowSize)

                    val minCount = window.minOfOrNull {
                        it.adjustResultList.size
                    } ?: 0

                    if (minCount == 0) continue

                    val startTime = window.first().time
                    val endTime = window.last().time.plusMinutes(30)

                    candidates.add(
                        RecommendedScheduleResponse(
                            dateInfo.date,
                            startTime,
                            endTime,
                            minCount
                        )
                    )
                }
            }
        }

        return candidates
            .sortedWith(
                compareByDescending<RecommendedScheduleResponse> {
                    it.availableCount
                }
                    .thenBy { it.date }
                    .thenBy { it.startTime }
            )
            .take(5)
    }

    private fun groupConsecutiveSlots(
        slots: List<TimeInfo>
    ): List<List<TimeInfo>> {

        if (slots.isEmpty()) {
            return emptyList()
        }

        val groups = mutableListOf<MutableList<TimeInfo>>()

        var current = mutableListOf(slots.first())

        for (i in 1 until slots.size) {

            val prev = slots[i - 1].time
            val curr = slots[i].time

            if (curr == prev.plusMinutes(30)) {

                current.add(slots[i])

            } else {

                groups.add(current)
                current = mutableListOf(slots[i])
            }
        }

        groups.add(current)

        return groups
    }
}