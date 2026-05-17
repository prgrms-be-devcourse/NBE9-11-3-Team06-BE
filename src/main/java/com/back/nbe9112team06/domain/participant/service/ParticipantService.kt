package com.back.nbe9112team06.domain.participant.service

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.meeting.service.MeetingService
import com.back.nbe9112team06.domain.participant.dto.request.ParticipantJoinRequest
import com.back.nbe9112team06.domain.participant.dto.response.ParticipantJoinResponse
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.domain.participant.repository.ParticipantRepository
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantService(
    private val meetingService: MeetingService,
    private val participantRepository: ParticipantRepository
) {

    @Transactional
    fun joinMeeting(randomUrl: String, request: ParticipantJoinRequest): ParticipantJoinResponse {
        val meeting = meetingService.getMeetingByRandomUrlOrThrow(randomUrl)

        val participant = Participant.create(request.guestName, request.guestPassword)
        meeting.addParticipant(participant)

        val saved = participantRepository.save(participant)
        return ParticipantJoinResponse(saved.id, saved.guestName)
    }

    @Transactional(readOnly = true)
    fun findParticipantOrThrow(meeting: Meeting, guestName: String, guestPassword: String): Participant =
        participantRepository.findByMeetingAndGuestNameAndGuestPassword(meeting, guestName, guestPassword)
            ?: throw BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND)

    @Transactional(readOnly = true)
    fun findParticipantByRandomUrlOrThrow(randomUrl: String, guestName: String, guestPassword: String): Participant {
        val meeting = meetingService.getMeetingByRandomUrlOrThrow(randomUrl)
        return findParticipantOrThrow(meeting, guestName, guestPassword)
    }

    @Transactional
    fun deleteParticipant(participant: Participant) {
        if (participant.id == 0 || !participantRepository.existsById(participant.id)) {
            throw BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND)
        }
        participantRepository.delete(participant)
    }
}
