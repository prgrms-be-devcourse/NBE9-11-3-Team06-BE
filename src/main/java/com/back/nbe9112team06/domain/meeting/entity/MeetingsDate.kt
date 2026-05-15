package com.back.nbe9112team06.domain.meeting.entity

import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class MeetingsDate() : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null
        protected set

    var date: LocalDate? = null
        protected set

    @Column(name = "created_by")
    var createdBy: String? = null
        protected set

    @Column(name = "modified_by")
    var modifiedBy: String? = null
        protected set

    fun assignMeeting(meeting: Meeting) {
        this.meeting = meeting
    }

    constructor(date: LocalDate, createdBy: String) : this() {
        this.date = date
        this.createdBy = createdBy
        this.modifiedBy = createdBy
    }
}
