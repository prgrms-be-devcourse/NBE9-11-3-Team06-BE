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

        // TODO: 모든 호출부가 Kotlin이므로 @JvmStatic 제거 가능
        @JvmStatic
        fun from(
            date: LocalDate,
            time: LocalTime,
            status: MeetingStatus,
            title: String,
            duration: Int
        ): ConfirmedScheduleResponse {
            val endTime = time.plusMinutes(duration.toLong()).format(TIME_FMT)
            val message = String.format(
                "📅 %s 일정이 확정되었습니다!\n\n• 날짜: %s\n• 시간: %s ~ %s",
                title, date, time.format(TIME_FMT), endTime
            )
            return ConfirmedScheduleResponse(date, time, message, status)
        }
    }
}
