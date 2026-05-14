package com.back.nbe9112team06.domain.timetable.entity;

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult;
import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class TimeInfo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "date_id")
    private DateInfo dateInfo;

    private LocalTime time;

    @OneToMany(mappedBy = "timeInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdjustResult> adjustResultList = new ArrayList<>();


    public TimeInfo(DateInfo dateInfo, LocalTime time) {
        this.dateInfo = dateInfo;
        this.time = time;
    }
}