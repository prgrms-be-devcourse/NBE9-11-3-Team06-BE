package com.back.nbe9112team06.domain.meeting.service;

import com.back.nbe9112team06.domain.meeting.dto.ConfirmedScheduleResponse;
import com.back.nbe9112team06.domain.meeting.dto.FinalizeRequest;
import com.back.nbe9112team06.domain.meeting.dto.request.MeetingCreateRequest;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingCreateResponse;
import com.back.nbe9112team06.domain.meeting.dto.response.MeetingEntryResponse;
import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus;
import com.back.nbe9112team06.domain.meeting.entity.MeetingsDate;
import com.back.nbe9112team06.domain.meeting.repository.MeetingRepository;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.service.MemberService;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final String URL_CHAR_POOL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int URL_LENGTH = 10;

    private final MeetingRepository meetingRepository;
    private final MemberService memberService;
    private final SecureRandom secureRandom = new SecureRandom();

    // ── 모임 생성 ──────────────────────────────
    @Transactional
    public MeetingCreateResponse createMeeting(Integer memberId, MeetingCreateRequest request) {

        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String randomUrl = generateUniqueUrl();
        Meeting meeting = Meeting.create(
                request.title(),
                request.category(),
                request.duration(),
                member,
                randomUrl
        );

        for (LocalDate date : request.dates()) {
            MeetingsDate meetingsDate = MeetingsDate.create(date, member.getEmail());
            meeting.addMeetingsDate(meetingsDate);
        }

        Meeting saved = meetingRepository.save(meeting);
        return new MeetingCreateResponse(saved.getId(), saved.getRandomUrl());
    }

    @Transactional(readOnly = true)
    public MeetingEntryResponse getMeetingByRandomUrl(String randomUrl) {
        Meeting meeting = findMeetingByRandomUrlInternal(randomUrl);

        List<LocalDate> dates = meeting.getMeetingsDates().stream()
                .map(MeetingsDate::getDate)
                .sorted()
                .toList();

        return new MeetingEntryResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getCategory(),
                meeting.getDuration(),
                meeting.getStatus(),
                meeting.getRandomUrl(),
                dates,
                meeting.getCreatedAt(),
                meeting.getConfirmedDate(),
                meeting.getConfirmedTime()
        );
    }

    // ── 모임 삭제 ──────────────────────────────
    @Transactional
    public void deleteMeeting(Integer meetingId, Integer memberId) {
        Meeting meeting = findMeetingInternal(meetingId);

        if (!meeting.isHost(memberId)) {
            throw new BusinessException(ErrorCode.NOT_MEETING_HOST);
        }

        meetingRepository.delete(meeting);
    }

    @Transactional(readOnly = true)
    public boolean checkIsHost(String randomUrl, Integer memberId) {
        Meeting meeting = findMeetingByRandomUrlInternal(randomUrl);
        return meeting.isHost(memberId);
    }

    // ── 일정 확정 ──────────────────────────────
    @Transactional
    public ConfirmedScheduleResponse confirm(Integer meetingId, Integer memberId, FinalizeRequest request) {
        Meeting meeting = findMeetingInternal(meetingId);

        if (!meeting.isHost(memberId)) {
            throw new BusinessException(ErrorCode.NOT_MEETING_HOST);
        }

        if (meeting.getParticipants().isEmpty()) {
            throw new BusinessException(ErrorCode.MEETING_NO_PARTICIPANTS);
        }

        if (meeting.getStatus() == MeetingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ALREADY_CONFIRMED);
        }

        meeting.confirm(request.date(), request.time());
        return ConfirmedScheduleResponse.from(request.date(), request.time(), MeetingStatus.CONFIRMED, meeting.getTitle(), meeting.getDuration());
    }

    @Transactional
    public void cancelConfirm(Integer meetingId, Integer memberId) {
        Meeting meeting = findMeetingInternal(meetingId);

        if (!meeting.isHost(memberId)) {
            throw new BusinessException(ErrorCode.NOT_MEETING_HOST);
        }

        if (meeting.getStatus() != MeetingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.NOT_CONFIRMED);
        }

        meeting.cancelConfirm();
    }

    @Transactional(readOnly = true)
    public ConfirmedScheduleResponse getConfirmedSchedule(Integer meetingId) {
        Meeting meeting = findMeetingInternal(meetingId);

        if (meeting.getStatus() != MeetingStatus.CONFIRMED || meeting.getConfirmedDate() == null) {
            throw new BusinessException(ErrorCode.NOT_CONFIRMED);
        }

        return ConfirmedScheduleResponse.from(
                meeting.getConfirmedDate(),
                meeting.getConfirmedTime(),
                meeting.getStatus(),
                meeting.getTitle(),
                meeting.getDuration()
        );
    }

    // ── 목록 조회 ──────────────────────────────
    @Transactional(readOnly = true)
    public List<MeetingEntryResponse> getMyMeetings(Integer memberId) {
        return meetingRepository.findByMember_IdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(meeting -> {
                    List<LocalDate> dates = meeting.getMeetingsDates().stream()
                            .map(MeetingsDate::getDate)
                            .sorted()
                            .toList();
                    return new MeetingEntryResponse(
                            meeting.getId(),
                            meeting.getTitle(),
                            meeting.getCategory(),
                            meeting.getDuration(),
                            meeting.getStatus(),
                            meeting.getRandomUrl(),
                            dates,
                            meeting.getCreatedAt(),
                            meeting.getConfirmedDate(),
                            meeting.getConfirmedTime()
                    );
                })
                .toList();
    }

    // 외부 유틸
    @Transactional(readOnly = true)
    public Meeting getMeetingOrThrow(Integer meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Meeting getMeetingByRandomUrlOrThrow(String randomUrl) {
        return meetingRepository.findByRandomUrl(randomUrl)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
    }

    // ── 내부 유틸 ──────────────────────────────
    private String generateUniqueUrl() {
        String candidate = randomString(URL_LENGTH);
        while (meetingRepository.existsByRandomUrl(candidate)) {
            candidate = randomString(URL_LENGTH);
        }
        return candidate;
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(URL_CHAR_POOL.length());
            builder.append(URL_CHAR_POOL.charAt(idx));
        }
        return builder.toString();
    }

    private Meeting findMeetingInternal(Integer meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
    }

    private Meeting findMeetingByRandomUrlInternal(String randomUrl) {
        return meetingRepository.findByRandomUrl(randomUrl)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));
    }
}