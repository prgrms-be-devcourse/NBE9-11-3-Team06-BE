package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalTime

@Entity
class AvailableTime(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "available_date_time_id")
    var availableDateTime: AvailableDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_block_id")
    var timeBlock: TimeBlock,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    var participant: Participant,

    var time: LocalTime,

    ) : BaseEntity() {

    companion object {
        fun create(
            availableDateTime: AvailableDateTime,
            timeBlock: TimeBlock,
            meeting: Meeting,
            participant: Participant,
            time: LocalTime,
        ): AvailableTime = AvailableTime(
            availableDateTime = availableDateTime,
            timeBlock = timeBlock,
            meeting = meeting,
            participant = participant,
            time = time,
        )
    }
}