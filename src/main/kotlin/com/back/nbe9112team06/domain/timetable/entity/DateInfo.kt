package com.back.nbe9112team06.domain.timetable.entity

import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import java.time.LocalDate

@Entity
class DateInfo(
    @JoinColumn(name = "time_table_id")
    @ManyToOne(fetch = FetchType.LAZY)
    var timeTable: TimeTable,
    var date: LocalDate
) : BaseEntity() {

    @OneToMany(mappedBy = "dateInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 100)
    val timeInfos: MutableList<TimeInfo> = mutableListOf()
}
