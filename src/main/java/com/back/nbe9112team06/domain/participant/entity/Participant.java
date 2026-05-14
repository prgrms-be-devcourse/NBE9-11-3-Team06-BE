package com.back.nbe9112team06.domain.participant.entity;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Participant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_password")
    private String guestPassword;

    public static Participant create(String guestName, String guestPassword) {
        Participant participant = new Participant();
        participant.guestName = guestName;
        participant.guestPassword = guestPassword;
        return participant;
    }

    public void assignMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}
