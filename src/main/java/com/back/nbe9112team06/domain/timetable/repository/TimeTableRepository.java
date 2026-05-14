package com.back.nbe9112team06.domain.timetable.repository;

import com.back.nbe9112team06.domain.timetable.entity.TimeTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TimeTableRepository extends JpaRepository<TimeTable, Integer> {
    List<TimeTable> findByMeeting_Id(Integer meetingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select tt
    from TimeTable tt
    where tt.meeting.id = :meetingId
""")
    Optional<TimeTable> findByMeetingIdForUpdate(Integer meetingId);
}
