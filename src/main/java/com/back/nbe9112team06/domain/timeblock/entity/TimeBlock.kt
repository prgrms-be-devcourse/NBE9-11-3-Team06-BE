package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne

@Entity
class TimeBlock(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    var participant: Participant,

    @Column(name = "created_by")
    var createdBy: String,

    ) : BaseEntity() {

    @OneToMany(mappedBy = "timeBlock", cascade = [CascadeType.ALL], orphanRemoval = true)
    val availableDateTimes: MutableList<AvailableDateTime> = mutableListOf()

    companion object {
        @JvmStatic
        fun create(meeting: Meeting, participant: Participant): TimeBlock =
            TimeBlock(
                meeting = meeting,
                participant = participant,
                createdBy = participant.guestName,
            )
    }
}