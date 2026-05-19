package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize

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
    @BatchSize(size = 100)
    val availableDateTimes: MutableList<AvailableDateTime> = mutableListOf()

    companion object {
        fun create(meeting: Meeting, participant: Participant): TimeBlock =
            TimeBlock(
                meeting = meeting,
                participant = participant,
                createdBy = participant.guestName,
            )
    }
}