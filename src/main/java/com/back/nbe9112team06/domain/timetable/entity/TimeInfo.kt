package com.back.nbe9112team06.domain.timetable.entity

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import lombok.NoArgsConstructor
import org.hibernate.annotations.BatchSize
import java.time.LocalTime

@Entity
@NoArgsConstructor
class TimeInfo(
    @field:JoinColumn(name = "date_id") @field:ManyToOne(fetch = FetchType.LAZY) var dateInfo: DateInfo,
    var time: LocalTime
) : BaseEntity() {

    @OneToMany(mappedBy = "timeInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    @BatchSize(size = 100)
    val adjustResultList: MutableList<AdjustResult> = mutableListOf()
}