package com.back.nbe9112team06.domain.timetable.entity;

import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class DateInfo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_table_id")
    private TimeTable timeTable;

    private LocalDate date;

    @OneToMany(mappedBy = "dateInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeInfo> timeInfos = new ArrayList<>();


    public DateInfo(TimeTable timeTable, LocalDate date) {
        this.timeTable = timeTable;
        this.date = date;
    }
}
