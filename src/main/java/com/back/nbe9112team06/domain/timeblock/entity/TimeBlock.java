package com.back.nbe9112team06.domain.timeblock.entity;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class TimeBlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @OneToMany(mappedBy = "timeBlock", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvailableDateTime> availableDateTimes = new ArrayList<>();

    @Column(name = "created_by")
    private String createdBy;

    public static TimeBlock create(Meeting meeting, Participant participant) {
        TimeBlock timeBlock = new TimeBlock();
        timeBlock.meeting = meeting;
        timeBlock.participant = participant;
        timeBlock.createdBy = participant.getGuestName();
        return timeBlock;
    }

}