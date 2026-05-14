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
class AvailableDateTime : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_block_id")
    var timeBlock: TimeBlock? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null
        protected set

    var date: LocalDate? = null
        protected set

    @OneToMany(mappedBy = "availableDateTime", cascade = [CascadeType.ALL], orphanRemoval = true)
    val availableTimes: MutableList<AvailableTime> = mutableListOf()

    @Column(name = "created_by")
    var createdBy: String? = null
        protected set

    companion object {
        @JvmStatic
        fun create(timeBlock: TimeBlock, meeting: Meeting, date: LocalDate): AvailableDateTime =
            AvailableDateTime().apply {
                this.timeBlock = timeBlock
                this.meeting = meeting
                this.date = date
                this.createdBy = timeBlock.participant?.guestName
            }
    }
}