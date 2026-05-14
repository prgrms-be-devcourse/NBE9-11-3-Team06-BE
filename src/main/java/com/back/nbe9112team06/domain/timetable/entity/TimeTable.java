package com.back.nbe9112team06.domain.timetable.entity;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class TimeTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @OneToMany(mappedBy = "timeTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DateInfo> dateInfos = new ArrayList<>();


    public TimeTable(Meeting meeting, List<DateInfo> dateInfos){
        this.meeting = meeting;
        this.dateInfos = dateInfos;
    }
}