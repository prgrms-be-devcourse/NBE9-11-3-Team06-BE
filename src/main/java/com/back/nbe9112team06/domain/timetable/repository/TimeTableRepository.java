package com.back.nbe9112team06.domain.timetable.repository;

import com.back.nbe9112team06.domain.timetable.entity.TimeTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeTableRepository extends JpaRepository<TimeTable, Integer> {
    List<TimeTable> findByMeeting_Id(Integer meetingId);
}
