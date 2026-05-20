package com.back.nbe9112team06.domain.timeblock.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
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

    ) : BaseEntity() {

    @OneToMany(mappedBy = "availableDateTime", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 50)
    val availableTimes: MutableList<AvailableTime> = mutableListOf()

    companion object {
        fun create(timeBlock: TimeBlock, meeting: Meeting, date: LocalDate): AvailableDateTime =
            AvailableDateTime(
                timeBlock = timeBlock,
                meeting = meeting,
                date = date,
            )
    }
}