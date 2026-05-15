package com.back.nbe9112team06.domain.timetable.entity

import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import lombok.NoArgsConstructor
import java.time.LocalDate

@Entity
@NoArgsConstructor
class DateInfo(
    @field:JoinColumn(name = "time_table_id") @field:ManyToOne(fetch = FetchType.LAZY)
    var timeTable: TimeTable,
    var date: LocalDate
) : BaseEntity() {

    @OneToMany(mappedBy = "dateInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    val timeInfos: MutableList<TimeInfo> = mutableListOf()
}
