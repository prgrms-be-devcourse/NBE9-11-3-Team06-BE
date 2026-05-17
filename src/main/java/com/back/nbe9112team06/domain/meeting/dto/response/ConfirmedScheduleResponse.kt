package com.back.nbe9112team06.domain.meeting.dto.response

import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ConfirmedScheduleResponse(
    val date: LocalDate,
    val time: LocalTime,
    val message: String,
    val status: MeetingStatus
) {
    companion object {
        private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun from(
            date: LocalDate,
            time: LocalTime,
            status: MeetingStatus,
            title: String,
            duration: Int
        ): ConfirmedScheduleResponse {
            val startFmt = time.format(TIME_FMT)
            val endFmt = time.plusMinutes(duration.toLong()).format(TIME_FMT)
            val message = "📅 $title 일정이 확정되었습니다!\n\n• 날짜: $date\n• 시간: $startFmt ~ $endFmt"
            return ConfirmedScheduleResponse(date, time, message, status)
        }
    }
}
