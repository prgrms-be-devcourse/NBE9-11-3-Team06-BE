package com.back.nbe9112team06.domain.timeblock.repository;

import com.back.nbe9112team06.domain.meeting.entity.Meeting;
import com.back.nbe9112team06.domain.participant.entity.Participant;
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TimeBlockRepository extends JpaRepository<TimeBlock, Integer> {

    // 중복등록체크 (같은 모임에 같은 이름으로 이미 등록했는지)
    Optional<TimeBlock> findByMeetingAndParticipant(Meeting meeting, Participant participant);

    // 타임블록에서 meetingId로 시간당 데이터 꺼내기
    @Query("""
                select distinct tb from TimeBlock tb
                join fetch tb.participant p
                join fetch tb.availableDateTimes d
                where tb.meeting.id = :meetingId
            """)
    List<TimeBlock> findWithAll(Integer meetingId);
}