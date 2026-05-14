package com.back.nbe9112team06.domain.meeting.dto.response

import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MeetingEntryResponse(
    val meetingId: Int,
    val title: String,
    val category: String,
    val duration: Int,
    val status: MeetingStatus,
    val roomUrl: String,
    val dates: MutableList<LocalDate>,
    val createdAt: LocalDateTime,
    val confirmedDate: LocalDate?,
    val confirmedTime: LocalTime?
)

