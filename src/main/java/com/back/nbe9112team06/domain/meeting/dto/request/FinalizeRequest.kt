package com.back.nbe9112team06.domain.meeting.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalTime

data class FinalizeRequest(
    val date: LocalDate,

    @JsonFormat(pattern = "HH:mm")
    val time: LocalTime
)
