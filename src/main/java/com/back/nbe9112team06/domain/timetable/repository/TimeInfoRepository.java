package com.back.nbe9112team06.domain.timetable.repository;

import com.back.nbe9112team06.domain.timetable.entity.TimeInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeInfoRepository extends JpaRepository<TimeInfo, Integer> {
}
