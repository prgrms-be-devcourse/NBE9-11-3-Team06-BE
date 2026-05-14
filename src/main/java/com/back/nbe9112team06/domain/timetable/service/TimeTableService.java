package com.back.nbe9112team06.domain.timetable.service;

import com.back.nbe9112team06.domain.adjustresult.entity.AdjustResult;
import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.meeting.service.MeetingService;
import com.back.nbe9112team06.domain.timeblock.entity.AvailableDateTime;
import com.back.nbe9112team06.domain.timeblock.entity.AvailableTime;
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock;
import com.back.nbe9112team06.domain.timeblock.repository.TimeBlockRepository;
import com.back.nbe9112team06.domain.timetable.dto.DateResponse;
import com.back.nbe9112team06.domain.timetable.dto.RecommendedScheduleResponse;
import com.back.nbe9112team06.domain.timetable.dto.TimeResponse;
import com.back.nbe9112team06.domain.timetable.dto.TimeTableResponse;
import com.back.nbe9112team06.domain.timetable.entity.DateInfo;
import com.back.nbe9112team06.domain.timetable.entity.TimeInfo;
import com.back.nbe9112team06.domain.timetable.entity.TimeTable;
import com.back.nbe9112team06.domain.timetable.repository.TimeTableRepository;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


/**
 * 현재 타 도메인의 JpaRepository 메서드를 그대로 사용 중
 * merge 이후 각 Service의 메서드로 수정 필요
 */
@Service
@RequiredArgsConstructor
public class TimeTableService {

    private final TimeTableRepository timeTableRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final MeetingService meetingService;

    @PersistenceContext
    private EntityManager em;

    // 개인 가능일정 통합 (완전히 삭제하고 새로 채우기)
    @Transactional
    public void aggregate(Integer meetingId) {

        Meeting meeting = meetingService.getMeetingOrThrow(meetingId);

        //기존 타임테이블 제거 후 생성, orphanRemoval
        deleteAllByMeetingId(meetingId);

        //해당 모임의 타임블록들 꺼내서 Map에 저장
        List<TimeBlock> timeBlocks = findWithAll(meetingId);
        Map<LocalDateTime, List<String>> timeToParticipantsNames = new HashMap<>();

        for (TimeBlock timeBlock : timeBlocks) {

            String participantName = timeBlock.getParticipant().getGuestName(); // timeBlock의 작성자 이름 저장

            for (AvailableDateTime availableDateTime : timeBlock.getAvailableDateTimes()) {
                for (AvailableTime availableTime : availableDateTime.getAvailableTimes()) {
                    LocalDate date = availableDateTime.getDate();
                    LocalTime time = availableTime.getTime();

                    LocalDateTime key = LocalDateTime.of(date, time);

                    // 같은 시간이 없으면 리스트 생성 후 참가자 이름 추가, 있으면 그냥 추가
                    timeToParticipantsNames
                            .computeIfAbsent(key, k -> new ArrayList<>())
                            .add(participantName);
                }


            }
        }

        //Meeting meeting = meetingRepository.findById(meetingId);
        //Meeting meeting = em.getReference(Meeting.class, meetingId);
        TimeTable timeTable = new TimeTable(meeting, new ArrayList<>());

        //key: 날짜   value: 시간당 이름 Map
        Map<LocalDate, List<Map.Entry<LocalDateTime, List<String>>>> dateMap = new HashMap<>();
        for (var entry : timeToParticipantsNames.entrySet()) {
            LocalDate date = entry.getKey().toLocalDate();

            dateMap
                    .computeIfAbsent(date, K -> new ArrayList<>())
                    .add(entry);
        }


        for (var dateEntry : dateMap.entrySet()) {

            LocalDate date = dateEntry.getKey();
            DateInfo dateInfo = new DateInfo(timeTable, date);
            timeTable.getDateInfos().add(dateInfo);

            for (var timeEntry : dateEntry.getValue()) {

                LocalDateTime dateTime = timeEntry.getKey();
                LocalTime time = dateTime.toLocalTime();

                TimeInfo timeInfo = new TimeInfo(dateInfo, time);
                dateInfo.getTimeInfos().add(timeInfo);

                for (String participantName : timeEntry.getValue()) {

                    AdjustResult adjustResult = new AdjustResult(timeInfo, participantName);
                    timeInfo.getAdjustResultList().add(adjustResult);
                }
            }
        }

        timeTableRepository.save(timeTable);
    }


    // meetingId로 TimeTable 검색
    public List<TimeTable> findByMeetingId(Integer meetingId) {

        return timeTableRepository.findByMeeting_Id(meetingId);
    }

    // 해당 모임의 TimeTable 초기화
    @Transactional
    public void deleteAllByMeetingId(Integer meetingId) {
        List<TimeTable> tables = timeTableRepository.findByMeeting_Id(meetingId);
        timeTableRepository.deleteAll(tables);
    }

    // 타임블록 DB 에서 데이터 꺼내기
    public List<TimeBlock> findWithAll(Integer meetingId) {
        List<TimeBlock> timeBlocks = timeBlockRepository.findWithAll(meetingId);

        if (timeBlocks.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        return timeBlocks;

    }

    // 미팅 ID로 TimeTable 반환
    @Transactional(readOnly = true)
    public TimeTableResponse getTimeTable(Integer meetingId) {

        List<TimeTable> tables = timeTableRepository.findByMeeting_Id(meetingId);

        // 테이블 없으면 빈 리스트 반환
        if (tables.isEmpty()) {
            return new TimeTableResponse(List.of());
        }

        TimeTable table = tables.get(0);

        List<DateResponse> dateResponses = new ArrayList<>();

        for (DateInfo dateInfo : table.getDateInfos()) {
            List<TimeResponse> timeResponses = new ArrayList<>();

            for (TimeInfo timeInfo : dateInfo.getTimeInfos()) {
                List<String> participants = timeInfo.getAdjustResultList()
                        .stream()
                        .map(AdjustResult::getName)
                        .toList();

                timeResponses
                        .add(new TimeResponse(
                                timeInfo.getTime(),
                                participants,
                                participants.size()
                        ));
            }

            // dto 시간 오름차순
            timeResponses.sort(Comparator.comparing(TimeResponse::time));

            dateResponses.add(new DateResponse(
                    dateInfo.getDate(),
                    timeResponses
            ));
        }

        // dto 날짜 오름차순
        dateResponses.sort(Comparator.comparing(DateResponse::availableDate));

        return new TimeTableResponse(dateResponses);
    }

    @Transactional(readOnly = true)
    public List<RecommendedScheduleResponse> recommend(Integer meetingId) {

        // 모임 전체 시간표 조회
        List<TimeTable> tables = timeTableRepository.findByMeeting_Id(meetingId);
        if (tables.isEmpty()) return List.of();

        // 최소 회의시간(duration)을 슬롯 개수(windowSize)로 변환
        TimeTable table = tables.get(0);
        int windowSize = table.getMeeting().getDuration() / 30;
        if (windowSize < 1) return List.of();

        // 추천 후보 저장
        List<RecommendedScheduleResponse> candidates = new ArrayList<>();

        // 날짜별로 추천 가능한 시간 찾기
        for (DateInfo dateInfo : table.getDateInfos()) {
            List<TimeInfo> slots = dateInfo.getTimeInfos().stream()
                    .sorted(Comparator.comparing(TimeInfo::getTime))
                    .toList();

            for (List<TimeInfo> group : groupConsecutiveSlots(slots)) {
                if (group.size() < windowSize) continue;

                // 슬라이딩 윈도우로 duration 길이만큼 후보 구간 생성
                for (int i = 0; i <= group.size() - windowSize; i++) {
                    List<TimeInfo> window = group.subList(i, i + windowSize);

                    // 구간 내 최소 참여 가능 인원 계산
                    int minCount = window.stream()
                            .mapToInt(ti -> ti.getAdjustResultList().size())
                            .min()
                            .orElse(0);

                    if (minCount == 0) continue;

                    LocalTime startTime = window.get(0).getTime();
                    LocalTime endTime = window.get(windowSize - 1).getTime().plusMinutes(30);

                    candidates.add(new RecommendedScheduleResponse(
                            dateInfo.getDate(), startTime, endTime, minCount
                    ));
                }
            }
        }

        // 참여 인원 많은 순 -> 빠른 날짜/시간 순으로 정렬 후 상위 5개 반환
        return candidates.stream()
                .sorted(Comparator.comparingInt(RecommendedScheduleResponse::availableCount).reversed()
                        .thenComparing(RecommendedScheduleResponse::date)
                        .thenComparing(RecommendedScheduleResponse::startTime))
                .limit(5)
                .toList();
    }

    private List<List<TimeInfo>> groupConsecutiveSlots(List<TimeInfo> slots) {
        List<List<TimeInfo>> groups = new ArrayList<>();
        if (slots.isEmpty()) return groups;

        List<TimeInfo> current = new ArrayList<>();
        current.add(slots.get(0));

        for (int i = 1; i < slots.size(); i++) {
            LocalTime prev = slots.get(i - 1).getTime();
            LocalTime curr = slots.get(i).getTime();
            if (curr.equals(prev.plusMinutes(30))) {
                current.add(slots.get(i));
            } else {
                groups.add(current);
                current = new ArrayList<>();
                current.add(slots.get(i));
            }
        }
        groups.add(current);
        return groups;
    }

}
