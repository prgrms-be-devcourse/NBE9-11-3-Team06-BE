package com.back.nbe9112team06.domain.timetable.repository

import com.back.nbe9112team06.domain.timetable.entity.TimeTable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface TimeTableRepository : JpaRepository<TimeTable, Int> {
    fun findByMeetingId(meetingId: Int): MutableList<TimeTable>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
    select tt
    from TimeTable tt
    where tt.meeting.id = :meetingId
    """
    )
    fun findByMeetingIdForUpdate(meetingId: Int): TimeTable?
}
