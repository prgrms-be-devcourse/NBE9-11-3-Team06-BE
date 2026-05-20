package com.back.nbe9112team06.domain.timetable.entity

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import java.time.LocalTime

@Entity
class TimeInfo(
    @JoinColumn(name = "date_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var dateInfo: DateInfo,
    var time: LocalTime
) : BaseEntity() {

    @OneToMany(mappedBy = "timeInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 50)
    val adjustResultList: MutableList<AdjustResult> = mutableListOf()
}