package com.back.nbe9112team06.domain.meeting.entity

import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class MeetingsDate(
    var date: LocalDate,

    @Column(name = "created_by")
    var createdBy: String,

    @Column(name = "modified_by")
    var modifiedBy: String = createdBy,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null

) : BaseEntity() {

    fun assignMeeting(meeting: Meeting) {
        this.meeting = meeting
    }
}
