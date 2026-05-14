package com.back.nbe9112team06.domain.participant.repository;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Integer> {

    Optional<Participant> findByMeetingAndGuestNameAndGuestPassword(
            Meeting meeting,
            String guestName,
            String guestPassword
    );
}

