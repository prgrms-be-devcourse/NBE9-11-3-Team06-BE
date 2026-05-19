package com.back.nbe9112team06.domain.timetable.service

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.domain.timeblock.repository.AvailableTimeRepository
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

@Service
class TimeTableService(
    private val timeTableRepository: TimeTableRepository,
    private val availableTimeRepository: AvailableTimeRepository
) {

    // 개인 가능일정 통합
    @Transactional
    fun aggregate(meetingId: Int) {

        val timeTable = timeTableRepository
            .findByMeetingId(meetingId)
            ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

        timeTable.dateInfos.clear()

        val availableTimes = availableTimeRepository
            .findByMeetingId(meetingId)

        availableTimes
            .groupBy { it.availableDateTime.date }
            .forEach { (date, timesByDate) ->

                val dateInfo = DateInfo(timeTable, date)
                    .also { timeTable.dateInfos.add(it) }

                timesByDate
                    .groupBy { it.time }
                    .forEach { (time, slotTimes) ->

                        val timeInfo = TimeInfo(dateInfo, time)
                            .also { dateInfo.timeInfos.add(it) }

                        slotTimes.forEach { availableTime ->

                            AdjustResult(
                                timeInfo,
                                availableTime.participant.guestName
                            ).also {
                                timeInfo.adjustResultList.add(it)
                            }
                        }
                    }
            }

        timeTableRepository.save(timeTable)
    }

    // timetable 저장
    fun save(timeTable: TimeTable) {
        timeTableRepository.save(timeTable)
    }


    // 미팅 ID로 TimeTable 반환
    @Transactional(readOnly = true)
    fun getTimeTable(meetingId: Int): TimeTableResponse {

        val table = timeTableRepository.findByMeetingId(meetingId)
            ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)


        val dateResponses = mutableListOf<DateResponse>()

        for (dateInfo in table.dateInfos) {

            val timeResponses = mutableListOf<TimeResponse>()

            for (timeInfo in dateInfo.timeInfos) {

                val participants = timeInfo.adjustResultList
                    .map { it.name }

                timeResponses.add(
                    TimeResponse(
                        timeInfo.time,
                        participants,
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

        val table = timeTableRepository.findByMeetingId(meetingId)
            ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

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

    fun groupConsecutiveSlots(
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