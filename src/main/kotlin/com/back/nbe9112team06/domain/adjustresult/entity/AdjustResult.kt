package com.back.nbe9112team06.domain.adjustresult.entity

import com.back.nbe9112team06.domain.timetable.entity.TimeInfo
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class AdjustResult(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_id")
    var timeInfo: TimeInfo,
    var participantId: Int
) : BaseEntity() {

    //    @ManyToOne(fetch = FetchType.LAZY)
    //    @JoinColumn(name = "date_id")
    //    private DateInfo dateInfo;
    //
    //    @ManyToOne(fetch = FetchType.LAZY)
    //    @JoinColumn(name = "time_table_id")
    //    private TimeTable timeTable;
    //
    //    @ManyToOne(fetch = FetchType.LAZY)
    //    @JoinColumn(name = "meeting_id")
    //    private Meeting meeting;
}
