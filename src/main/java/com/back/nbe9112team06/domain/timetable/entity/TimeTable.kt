package com.back.nbe9112team06.domain.timetable.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
class TimeTable() : BaseEntity() {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", unique = true)
    lateinit var meeting: Meeting

    @OneToMany(
        mappedBy = "timeTable",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
     var dateInfos: MutableList<DateInfo> = mutableListOf()

    constructor(
        meeting: Meeting,
        dateInfos: MutableList<DateInfo>
    ) : this() {
        this.meeting = meeting
        this.dateInfos = dateInfos
    }


}