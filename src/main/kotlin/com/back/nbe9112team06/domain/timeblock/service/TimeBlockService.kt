package com.back.nbe9112team06.domain.timeblock.service

import com.back.nbe9112team06.domain.meeting.service.MeetingService
import com.back.nbe9112team06.domain.participant.service.ParticipantService
import com.back.nbe9112team06.domain.timeblock.dto.TimeBlockRequest
import com.back.nbe9112team06.domain.timeblock.dto.TimeRangeResponse
import com.back.nbe9112team06.domain.timeblock.dto.request.TimeBlockDeleteRequest
import com.back.nbe9112team06.domain.timeblock.dto.response.ParticipantsScheduleResponse
import com.back.nbe9112team06.domain.timeblock.entity.AvailableDateTime
import com.back.nbe9112team06.domain.timeblock.entity.AvailableTime
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock
import com.back.nbe9112team06.domain.timeblock.repository.AvailableDateTimeRepository
import com.back.nbe9112team06.domain.timeblock.repository.AvailableTimeRepository
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository
import com.back.nbe9112team06.domain.timetable.service.TimeTableService
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class TimeBlockService(
    private val meetingService: MeetingService,
    private val participantService: ParticipantService,
    private val timeBlockRepository: TimeBlockRepository,
    private val availableDateTimeRepository: AvailableDateTimeRepository,
    private val availableTimeRepository: AvailableTimeRepository,
    private val timeTableService: TimeTableService
) {

    // 타임블록 등록
    @Transactional
    fun registerTimeBlock(meetingId: Int, request: TimeBlockRequest) {
        // 이 모임이 존재하는지 (Meeting 존재)
        val meeting = meetingService.getMeetingOrThrow(meetingId)

        // 요청한 사람이 이 모임 참여자인지 (Participant 인증)
        val participant = participantService.findParticipantOrThrow(
            meeting,
            request.guestName,
            request.guestPassword,
        )

        // 시간표를 등록한 적이 있는지 (TimeBlock 중복)
        timeBlockRepository.findByMeetingAndParticipant(meeting, participant)?.let {
            throw BusinessException(ErrorCode.DUPLICATE_RESOURCE, "시간표가 이미 등록되었습니다.")
        }

        // availableDateTime 검증
        validateAvailableDateTime(request.availableDateTimes)

        // 날짜별 가능한 시간 목록 묶어서 Map 반환
        val dateTimeMap = buildDateTimeMap(request.availableDateTimes)

        // TimeBlock 저장
        val timeBlock = TimeBlock.create(meeting, participant)
            .also { timeBlockRepository.save(it) }

        // AvailableDateTime, AvailableTime 저장
        dateTimeMap.forEach { (date, times) ->

            val availableDateTime =
                AvailableDateTime.create(timeBlock, meeting, date)
            timeBlock.availableDateTimes.add(availableDateTime)
            availableDateTimeRepository.save(availableDateTime)

            times.forEach { time ->

                val availableTime =
                    AvailableTime.create(availableDateTime, timeBlock, meeting, timeBlock.participant, time)
                availableDateTime.availableTimes.add(availableTime)
                availableTimeRepository.save(availableTime)
            }
        }
        // 생성 후 aggregate
        timeTableService.aggregate(meetingId)
    }

    // 타임블록 삭제
    @Transactional
    fun deleteTImeBlock(meetingId: Int, request: TimeBlockDeleteRequest) {
        // Meeting 존재 여부 확인
        val meeting = meetingService.getMeetingOrThrow(meetingId)

        // 요청한 사람이 이 모임 참여자인지 (Participant 인증)
        val participant = participantService.findParticipantOrThrow(
            meeting,
            request.guestName,
            request.guestPassword,
        )

        // 삭제할 TimeBlock 가 없으면 예외
        val timeBlock = timeBlockRepository.findByMeetingAndParticipant(meeting, participant)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "삭제할 시간이 없습니다.")

        // TimeBlock 먼저 삭제 (participant_id FK 제거), 이후 Participant 삭제
        timeBlockRepository.delete(timeBlock)
        participantService.deleteParticipant(participant)

        // 삭제 후 aggregate
        timeTableService.aggregate(meetingId)
    }

    // 참여자 목록
    @Transactional(readOnly = true)
    fun getParticipantSchedules(meetingId: Int): List<ParticipantsScheduleResponse> {
        val timeBlocks = timeBlockRepository.findByMeetingId(meetingId)

        return timeBlocks.map { timeBlock ->
            val name = timeBlock.participant.guestName

            // 날짜별로 시간 슬롯 모으기 (날짜 정렬 보장 위해 TreeMap)
            val dateToSlots = timeBlock.availableDateTimes
                .flatMap { adt -> adt.availableTimes.map { adt.date to it.time } }
                .groupBy({ it.first }, { it.second })
                .toSortedMap()

            // 날짜별로 연속된 시간끼리 range 묶기
            val ranges = dateToSlots.flatMap { (date, slots) ->
                toRanges(date, slots.sorted())
            }

            ParticipantsScheduleResponse(name, ranges)
        }
    }

    // 30분 단위 슬롯을 연속 구간 묶음
    internal fun toRanges(date: LocalDate, slots: List<LocalTime>): List<TimeRangeResponse> {
        if (slots.isEmpty()) return emptyList()

        val ranges = mutableListOf<TimeRangeResponse>()
        var start = slots[0]
        var prev = slots[0]

        for (i in 1 until slots.size) {
            val curr = slots[i]
            // 이전 시간이랑 30분 차이가 안 나면 끊긴거니까 새 range 시작
            if (curr != prev.plusMinutes(30)) {
                ranges += TimeRangeResponse(date, start, prev.plusMinutes(30))
                start = curr
            }
            prev = curr
        }
        // 마지막 구간 마무리
        ranges += TimeRangeResponse(date, start, prev.plusMinutes(30))
        return ranges
    }

    // 검증 메서드
    internal fun validateAvailableDateTime(availableDateTimes: List<String>) {
        // 중복 검증
        if (availableDateTimes.toSet().size != availableDateTimes.size) {
            throw BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "시간 선택이 중복되었습니다.")
        }

        val now = LocalDateTime.now()

        availableDateTimes.forEach { dateTimeStr ->
            // 날짜 형식 검증
            val dateTime = try {
                LocalDateTime.parse(dateTimeStr, FORMATTER)
            } catch (e: DateTimeParseException) {
                throw BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "올바른 날짜 형식이 아닙니다.")
            }

            // 과거 날짜 검증
            if (dateTime.isBefore(now)) {
                throw BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "현재 날짜보다 과거 날짜는 선택할 수 없습니다.",
                )
            }

            // 30분 단위 검증
            if (dateTime.minute % 30 != 0) {
                throw BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "30분 단위 시간이 아닙니다.")
            }
        }
    }

    // 날짜별로 가능한 시간 목록 묶어서 Map 반환
    internal fun buildDateTimeMap(availableDateTimes: List<String>): Map<LocalDate, List<LocalTime>> =
        availableDateTimes
            .map { LocalDateTime.parse(it, FORMATTER) }
            .groupBy({ it.toLocalDate() }, { it.toLocalTime() })

    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}