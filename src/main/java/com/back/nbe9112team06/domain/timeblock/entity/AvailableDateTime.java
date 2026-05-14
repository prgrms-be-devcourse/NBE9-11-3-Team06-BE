package com.back.nbe9112team06.domain.timeblock.entity;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
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
public class AvailableDateTime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_block_id")
    private TimeBlock timeBlock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    private LocalDate date;

    @OneToMany(mappedBy = "availableDateTime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvailableTime> availableTimes = new ArrayList<>();

    @Column(name = "created_by")
    private String createdBy;

    public static AvailableDateTime create(TimeBlock timeBlock, Meeting meeting, LocalDate date) {
        AvailableDateTime availableDateTime = new AvailableDateTime();
        availableDateTime.timeBlock = timeBlock;
        availableDateTime.meeting = meeting;
        availableDateTime.date = date;
        availableDateTime.createdBy = timeBlock.getParticipant().getGuestName();
        return availableDateTime;
    }

}