package com.back.nbe9112team06.domain.timetable.repository

import com.back.nbe9112team06.domain.timetable.entity.TimeTable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface TimeTableRepository : JpaRepository<TimeTable, Int> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByMeetingId(meetingId: Int): TimeTable?
}
