package com.back.nbe9112team06.domain.meeting.entity

import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class MeetingsDate : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    var meeting: Meeting? = null

    var date: LocalDate? = null

    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "modified_by")
    var modifiedBy: String? = null

    fun assignMeeting(meeting: Meeting) {
        this.meeting = meeting
    }

    companion object {
        @JvmStatic
        fun create(date: LocalDate, createdBy: String): MeetingsDate {
            val meetingsDate = MeetingsDate()
            meetingsDate.date = date
            meetingsDate.createdBy = createdBy
            meetingsDate.modifiedBy = createdBy
            return meetingsDate
        }
    }
}
