package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
class TimeBlock : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    lateinit var participant: Participant
        protected set

    @OneToMany(mappedBy = "timeBlock", cascade = [CascadeType.ALL], orphanRemoval = true)
    val availableDateTimes: MutableList<AvailableDateTime> = mutableListOf()

    @Column(name = "created_by")
    var createdBy: String? = null
        protected set

    companion object {
        @JvmStatic
        fun create(meeting: Meeting, participant: Participant): TimeBlock =
            TimeBlock().apply {
                this.meeting = meeting
                this.participant = participant
                this.createdBy = participant.guestName
            }
    }
}