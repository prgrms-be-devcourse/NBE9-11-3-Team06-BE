package com.back.nbe9112team06.domain.participant.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Participant(
    @Column(name = "guest_name")
    var guestName: String,

    @Column(name = "guest_password")
    var guestPassword: String
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null

    fun assignMeeting(meeting: Meeting) {
        this.meeting = meeting
    }

    companion object {
        fun create(guestName: String, guestPassword: String): Participant {
            return Participant(guestName, guestPassword)
        }
    }
}
