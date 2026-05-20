package com.back.nbe9112team06.domain.timetable.dto

interface TimeSlotProjection {
    val date: java.time.LocalDate
    val time: java.time.LocalTime
    val participantId: Int
}