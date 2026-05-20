package com.back.nbe9112team06.domain.timeblock.dto

import java.time.LocalDate
import java.time.LocalTime

interface ParticipantScheduleSlot {
    val date: LocalDate
    val time: LocalTime
    val participantName: String  //  participant.guestName
}