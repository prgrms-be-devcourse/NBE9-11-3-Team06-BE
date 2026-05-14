package com.back.nbe9112team06.domain.participant.service;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.service.MeetingService;
import com.back.nbe9112team06.domain.participant.dto.request.ParticipantJoinRequest;
import com.back.nbe9112team06.domain.participant.dto.response.ParticipantJoinResponse;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.domain.participant.repository.ParticipantRepository;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final MeetingService meetingService;
    private final ParticipantRepository participantRepository;

    @Transactional
    public ParticipantJoinResponse joinMeeting(String randomUrl, ParticipantJoinRequest request) {
        Meeting meeting = meetingService.getMeetingByRandomUrlOrThrow(randomUrl);

        Participant participant = Participant.create(request.guestName(), request.guestPassword());
        meeting.addParticipant(participant);

        Participant saved = participantRepository.save(participant);
        return new ParticipantJoinResponse(saved.getId(), saved.getGuestName());
    }

    @Transactional(readOnly = true)
    public Participant findParticipantOrThrow(Meeting meeting, String guestName, String guestPassword) {
        return participantRepository.findByMeetingAndGuestNameAndGuestPassword(
                        meeting, guestName, guestPassword)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Participant findParticipantByRandomUrlOrThrow(
            String randomUrl, String guestName, String guestPassword) {

        Meeting meeting = meetingService.getMeetingByRandomUrlOrThrow(randomUrl);
        return findParticipantOrThrow(meeting, guestName, guestPassword);
    }

    @Transactional
    public void deleteParticipant(Participant participant) {
        if (participant.getId() == null || !participantRepository.existsById(participant.getId())) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        participantRepository.delete(participant);
    }
}
