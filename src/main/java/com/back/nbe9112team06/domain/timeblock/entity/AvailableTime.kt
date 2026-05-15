package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalTime

@Entity
class AvailableTime : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "available_date_time_id")
    var availableDateTime: AvailableDateTime? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_block_id")
    var timeBlock: TimeBlock? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null
        protected set

    var time: LocalTime? = null
        protected set

    @Column(name = "created_by")
    var createdBy: String? = null
        protected set

    companion object {
        @JvmStatic
        fun create(
            availableDateTime: AvailableDateTime,
            timeBlock: TimeBlock,
            meeting: Meeting,
            time: LocalTime,
        ): AvailableTime = AvailableTime().apply {
            this.availableDateTime = availableDateTime
            this.timeBlock = timeBlock
            this.meeting = meeting
            this.time = time
            this.createdBy = timeBlock.participant?.guestName
        }
    }
}