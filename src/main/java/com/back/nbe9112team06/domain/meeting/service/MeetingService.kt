package com.back.nbe9112team06.domain.meeting.service

import com.back.nbe9112team06.domain.meeting.dto.request.FinalizeRequest
import com.back.nbe9112team06.domain.meeting.dto.request.MeetingCreateRequest
import com.back.nbe9112team06.domain.meeting.dto.response.ConfirmedScheduleResponse
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingCreateResponse
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingEntryResponse
import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus
import com.back.nbe9112team06.domain.meeting.entity.MeetingsDate
import com.back.nbe9112team06.domain.meeting.repository.MeetingRepository
import com.back.nbe9112team06.domain.member.service.MemberService
import com.back.nbe9112team06.domain.timetable.entity.TimeTable
import com.back.nbe9112team06.domain.timetable.service.TimeTableService
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class MeetingService(
    private val meetingRepository: MeetingRepository,
    private val memberService: MemberService,
    private val timeTableService: TimeTableService
) {
    private val secureRandom = SecureRandom()

    // ── 모임 생성 ──────────────────────────────
    @Transactional
    fun createMeeting(memberId: Int, request: MeetingCreateRequest): MeetingCreateResponse {
        // TODO: Phase 2 - MemberService.findById()를 Optional 대신 Kotlin nullable 반환으로 변경하고 orElseThrow 제거
        val member = memberService.findById(memberId)
            .orElseThrow { BusinessException(ErrorCode.MEMBER_NOT_FOUND) }

        val randomUrl = generateUniqueUrl()
        val meeting = Meeting(
            request.title,
            request.category,
            request.duration,
            member,
            randomUrl
        )

        request.dates.forEach { date ->
            meeting.addMeetingsDate(MeetingsDate(date, member.email))
        }

        val saved = meetingRepository.save(meeting)

        val timeTable = TimeTable(meeting, mutableListOf())
        timeTableService.save(timeTable)

        return MeetingCreateResponse(saved.id, saved.randomUrl)
    }

    @Transactional(readOnly = true)
    fun getMeetingByRandomUrl(randomUrl: String): MeetingEntryResponse =
        findMeetingByRandomUrlInternal(randomUrl).toEntryResponse()

    // ── 모임 삭제 ──────────────────────────────
    @Transactional
    fun deleteMeeting(meetingId: Int, memberId: Int) {
        val meeting = findMeetingInternal(meetingId)
        meeting.validateHost(memberId)
        meetingRepository.delete(meeting)
    }

    @Transactional(readOnly = true)
    fun checkIsHost(randomUrl: String, memberId: Int): Boolean {
        val meeting = findMeetingByRandomUrlInternal(randomUrl)
        return meeting.isHost(memberId)
    }

    // ── 일정 확정 ──────────────────────────────
    @Transactional
    fun confirm(meetingId: Int, memberId: Int, request: FinalizeRequest): ConfirmedScheduleResponse {
        val meeting = findMeetingInternal(meetingId)
        meeting.validateHost(memberId)

        if (meeting.participants.isEmpty()) {
            throw BusinessException(ErrorCode.MEETING_NO_PARTICIPANTS)
        }

        if (meeting.status == MeetingStatus.CONFIRMED) {
            throw BusinessException(ErrorCode.ALREADY_CONFIRMED)
        }

        meeting.confirm(request.date, request.time)
        return ConfirmedScheduleResponse.from(
            request.date, request.time, MeetingStatus.CONFIRMED, meeting.title, meeting.duration
        )
    }

    @Transactional
    fun cancelConfirm(meetingId: Int, memberId: Int) {
        val meeting = findMeetingInternal(meetingId)
        meeting.validateHost(memberId)

        if (meeting.status != MeetingStatus.CONFIRMED) {
            throw BusinessException(ErrorCode.NOT_CONFIRMED)
        }

        meeting.cancelConfirm()
    }

    @Transactional(readOnly = true)
    fun getConfirmedSchedule(meetingId: Int): ConfirmedScheduleResponse {
        val meeting = findMeetingInternal(meetingId)

        if (meeting.status != MeetingStatus.CONFIRMED || meeting.confirmedDate == null) {
            throw BusinessException(ErrorCode.NOT_CONFIRMED)
        }

        return ConfirmedScheduleResponse.from(
            meeting.confirmedDate!!,
            meeting.confirmedTime!!,
            meeting.status,
            meeting.title,
            meeting.duration
        )
    }

    // ── 목록 조회 ──────────────────────────────
    @Transactional(readOnly = true)
    fun getMyMeetings(memberId: Int): List<MeetingEntryResponse> =
        meetingRepository.findByMember_IdOrderByCreatedAtDesc(memberId)
            .map { it.toEntryResponse() }

    // 외부 유틸
    @Transactional(readOnly = true)
    fun getMeetingOrThrow(meetingId: Int): Meeting =
        meetingRepository.findByIdOrNull(meetingId) ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

    @Transactional(readOnly = true)
    fun getMeetingByRandomUrlOrThrow(randomUrl: String): Meeting =
        meetingRepository.findByRandomUrl(randomUrl) ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

    // ── 내부 유틸 ──────────────────────────────
    private fun Meeting.validateHost(memberId: Int) {
        if (!isHost(memberId)) throw BusinessException(ErrorCode.NOT_MEETING_HOST)
    }

    private fun Meeting.toEntryResponse(): MeetingEntryResponse {
        val dates = meetingsDates.map { it.date }.sorted()
        return MeetingEntryResponse(
            id,
            title,
            category,
            duration,
            status,
            randomUrl,
            dates,
            createdAt,
            confirmedDate,
            confirmedTime
        )
    }

    private fun generateUniqueUrl(): String =
        generateSequence { randomString(URL_LENGTH) }
            .first { !meetingRepository.existsByRandomUrl(it) }

    private fun randomString(length: Int): String {
        val builder = StringBuilder(length)
        repeat(length) {
            val idx = secureRandom.nextInt(URL_CHAR_POOL.length)
            builder.append(URL_CHAR_POOL[idx])
        }
        return builder.toString()
    }

    private fun findMeetingInternal(meetingId: Int): Meeting =
        meetingRepository.findByIdOrNull(meetingId) ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

    private fun findMeetingByRandomUrlInternal(randomUrl: String): Meeting =
        meetingRepository.findByRandomUrl(randomUrl) ?: throw BusinessException(ErrorCode.MEETING_NOT_FOUND)

    companion object {
        private const val URL_CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val URL_LENGTH = 10
    }
}
