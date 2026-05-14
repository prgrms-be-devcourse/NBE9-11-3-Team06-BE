package com.back.nbe9112team06.domain.timeblock.service;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.service.MeetingService;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.domain.participant.service.ParticipantService;
import com.back.nbe9112team06.domain.timeblock.dto.response.ParticipantsScheduleResponse;
import com.back.nbe9112team06.domain.timeblock.dto.request.TimeBlockDeleteRequest;
import com.back.nbe9112team06.domain.timeblock.dto.TimeBlockRequest;
import com.back.nbe9112team06.domain.timeblock.dto.TimeRangeResponse;
import com.back.nbe9112team06.domain.timeblock.entity.AvailableDateTime;
import com.back.nbe9112team06.domain.timeblock.entity.AvailableTime;
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock;
import com.back.nbe9112team06.domain.timeblock.repository.AvailableDateTimeRepository;
import com.back.nbe9112team06.domain.timeblock.repository.AvailableTimeRepository;
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TimeBlockService {

    private final MeetingService meetingService;
    private final ParticipantService participantService;
    private final TimeBlockRepository timeBlockRepository;
    private final AvailableDateTimeRepository availableDateTimeRepository;
    private final AvailableTimeRepository availableTimeRepository;

    // ── 타임블록 등록 ──────────────────────────────
    @Transactional
    public void registerTimeBlock(Integer meetingId, TimeBlockRequest request) {
        // 이 모임이 존재하는지 (Meeting 존재)
        Meeting meeting = meetingService.getMeetingOrThrow(meetingId);

        // 요청한 사람이 이 모임 참여자인지 (Participant 인증)
        Participant participant = participantService.findParticipantOrThrow(
                meeting,
                request.getGuestName(),
                request.getGuestPassword());

        // 시간표를 등록한 적이 있는지 (TimeBlock 중복)
        timeBlockRepository.findByMeetingAndParticipant(meeting, participant)
                .ifPresent(timeBlock -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "시간표가 이미 등록되었습니다.");
                });

        // availableDateTime 테이블 검증
        validateAvailableDateTime(request.getAvailableDateTimes());

        //날짜별 가능한 시간 목록 묶어서 Map 반환 메서드 사용
        Map<LocalDate, List<LocalTime>> dateTimeMap = buildDateTimeMap(request.getAvailableDateTimes());

        // TimeBlock 저장
        TimeBlock timeBlock = TimeBlock.create(meeting, participant);
        timeBlockRepository.save(timeBlock);

        // AvailableDateTime 및 AvailableTime저장
        for(Map.Entry<LocalDate, List<LocalTime>> entry : dateTimeMap.entrySet()) {

            AvailableDateTime availableDateTime = AvailableDateTime.create(timeBlock, meeting, entry.getKey());
            availableDateTimeRepository.save(availableDateTime);

            for(LocalTime time : entry.getValue()){
                AvailableTime availableTime = AvailableTime.create(availableDateTime, timeBlock, meeting, time);
                availableTimeRepository.save(availableTime);
            }
        }
    }

    // ── 타임블록 삭제 ──────────────────────────────
    //삭제 메서드
    @Transactional
    public void deleteTImeBlock(Integer meetingId, TimeBlockDeleteRequest timeBlockDeleteRequest){
        //  Meeting 존재 여부 확인
        Meeting meeting = meetingService.getMeetingOrThrow(meetingId);
        // 요청한 사람이 이 모임 참여자인지 (Participant 인증)
        Participant participant = participantService.findParticipantOrThrow(
                meeting,
                timeBlockDeleteRequest.getGuestName(),
                timeBlockDeleteRequest.getGuestPassword());

        // 삭제할 TimeBlock가 없음
        TimeBlock timeBlock = timeBlockRepository.findByMeetingAndParticipant(meeting, participant)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "삭제할 시간이 없습니다."));

        // TimeBlock 먼저 삭제 (participant_id FK 제거), 이후 Participant 삭제
        timeBlockRepository.delete(timeBlock);
        participantService.deleteParticipant(participant);
    }

    // ── 참여자 목록 ──────────────────────────────
    @Transactional(readOnly = true)
    public List<ParticipantsScheduleResponse> getParticipantSchedules(Integer meetingId) {
        List<TimeBlock> timeBlocks = timeBlockRepository.findWithAll(meetingId);
        List<ParticipantsScheduleResponse> result = new ArrayList<>();

        for (TimeBlock timeBlock : timeBlocks) {
            String name = timeBlock.getParticipant().getGuestName();

            Map<LocalDate, List<LocalTime>> dateToSlots = new TreeMap<>();
            for (AvailableDateTime adt : timeBlock.getAvailableDateTimes()) {
                adt.getAvailableTimes().stream()
                        .map(AvailableTime::getTime)
                        .forEach(t -> dateToSlots
                                .computeIfAbsent(adt.getDate(), k -> new ArrayList<>())
                                .add(t));
            }

            List<TimeRangeResponse> ranges = new ArrayList<>();
            for (var entry : dateToSlots.entrySet()) {
                List<LocalTime> sorted = entry.getValue().stream().sorted().toList();
                ranges.addAll(toRanges(entry.getKey(), sorted));
            }

            result.add(new ParticipantsScheduleResponse(name, ranges));
        }

        return result;
    }

    private List<TimeRangeResponse> toRanges(LocalDate date, List<LocalTime> slots) {
        List<TimeRangeResponse> ranges = new ArrayList<>();
        if (slots.isEmpty()) return ranges;

        LocalTime start = slots.get(0);
        LocalTime prev = slots.get(0);

        for (int i = 1; i < slots.size(); i++) {
            LocalTime curr = slots.get(i);
            if (!curr.equals(prev.plusMinutes(30))) {
                ranges.add(new TimeRangeResponse(date, start, prev.plusMinutes(30)));
                start = curr;
            }
            prev = curr;
        }
        ranges.add(new TimeRangeResponse(date, start, prev.plusMinutes(30)));
        return ranges;
    }

    // ── 내부 유틸 ──────────────────────────────
    // 검증 메서드
    private void validateAvailableDateTime(List<String> availableDateTimes){
        // 중복 검증
        Set<String> set = new HashSet<>(availableDateTimes);
        if(set.size() != availableDateTimes.size()){
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "시간 선택이 중복되었습니다.");
        }

        // 날짜 형식 검증
        for(String dateTimeStr : availableDateTimes){
            LocalDateTime dateTime;
            try {
                dateTime = LocalDateTime.parse(dateTimeStr, // 문자열을 LocalDateTime 객체로 변환
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "올바른 날짜 형식이 아닙니다.");
            }

            // 과거 날짜 검증
            if(dateTime.isBefore(LocalDateTime.now())){
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "현재 날짜보다 과거 날짜는 선택할 수 없습니다.");
            }

            // 30분 단위 검증
            if(dateTime.getMinute()%30 != 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "30분 단위 시간이 아닙니다.");
            }
        }
    }

    // 날짜 별로 가능한 시간 목록 묶어서 Map으로 반환
    private Map<LocalDate, List<LocalTime>> buildDateTimeMap(List<String> availableDateTimes){
        // 날짜별로 시간 리스트 묶어서 Map 반환
        Map<LocalDate, List<LocalTime>> map = new HashMap<>();
        for(String dateTimeStr : availableDateTimes){
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            LocalDate date = dateTime.toLocalDate();
            LocalTime time = dateTime.toLocalTime();

            // 날짜 키가 없으면 새 리스트 생성 후 시간 추가
            if(!map.containsKey(date)){
                map.put(date, new ArrayList<>());
            }
            map.get(date).add(time);
        }
        return map;
    }

}
