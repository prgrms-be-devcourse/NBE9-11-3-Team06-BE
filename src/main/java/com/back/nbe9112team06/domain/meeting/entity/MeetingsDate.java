package com.back.nbe9112team06.domain.meeting.entity;

import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class MeetingsDate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    private LocalDate date;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_by")
    private String modifiedBy;

    public static MeetingsDate create(LocalDate date, String createdBy) {
        MeetingsDate meetingsDate = new MeetingsDate();
        meetingsDate.date = date;
        meetingsDate.createdBy = createdBy;
        meetingsDate.modifiedBy = createdBy;
        return meetingsDate;
    }

    public void assignMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}
