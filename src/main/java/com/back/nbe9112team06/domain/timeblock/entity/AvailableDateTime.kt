package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDate

@Entity
class AvailableDateTime(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_block_id")
    var timeBlock: TimeBlock,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting,

    var date: LocalDate,

    @Column(name = "created_by")
    var createdBy: String,

    ) : BaseEntity() {

    @OneToMany(mappedBy = "availableDateTime", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 100)
    val availableTimes: MutableList<AvailableTime> = mutableListOf()

    companion object {
        @JvmStatic
        fun create(timeBlock: TimeBlock, meeting: Meeting, date: LocalDate): AvailableDateTime =
            AvailableDateTime(
                timeBlock = timeBlock,
                meeting = meeting,
                date = date,
                createdBy = timeBlock.participant.guestName,
            )
    }
}