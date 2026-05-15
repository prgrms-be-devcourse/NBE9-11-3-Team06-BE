package com.back.nbe9112team06.domain.timetable.entity

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import lombok.NoArgsConstructor
import java.time.LocalTime

@Entity
@NoArgsConstructor
class TimeInfo(
    @field:JoinColumn(name = "date_id") @field:ManyToOne(fetch = FetchType.LAZY) var dateInfo: DateInfo,
    var time: LocalTime
) : BaseEntity() {

    @OneToMany(mappedBy = "timeInfo", cascade = [CascadeType.ALL], orphanRemoval = true)
    val adjustResultList: MutableList<AdjustResult> = ArrayList<AdjustResult>()
}