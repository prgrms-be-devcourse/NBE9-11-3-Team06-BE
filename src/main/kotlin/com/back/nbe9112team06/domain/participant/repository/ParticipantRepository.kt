package com.back.nbe9112team06.domain.participant.repository

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.participant.entity.Participant
import org.springframework.data.jpa.repository.JpaRepository
interface ParticipantRepository : JpaRepository<Participant, Int> {

    fun findByMeetingAndGuestNameAndGuestPassword(
        meeting: Meeting,
        guestName: String,
        guestPassword: String
    ): Participant?
}
