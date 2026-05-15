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
class TimeBlock : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    var participant: Participant? = null
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