package com.back.nbe9112team06.domain.timeblock.repository

import com.back.nbe9112team06.domain.timeblock.dto.ParticipantScheduleSlot
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock
import com.back.nbe9112team06.domain.timetable.dto.TimeSlotProjection
import com.back.nbe9112team06.domain.timetable.dto.TimeSlotWithMetaProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TimeBlockRepository : JpaRepository<TimeBlock, Int> {
    @Query("""
        SELECT 
            at.participant.id as participantId,
            at.availableDateTime.date as date, 
            at.time as time
        FROM AvailableTime at
        WHERE at.meeting.id = :meetingId
        ORDER BY at.availableDateTime.date, at.time
    """)
    fun findTimeSlotsForAggregation(@Param("meetingId") meetingId: Int): List<TimeSlotProjection>

    // 중복 등록 체크 (단순 존재 여부)
    fun findByMeetingIdAndParticipantId(meetingId: Int, participantId: Int): TimeBlock?

    // 참가자 인증용 (Meeting + Participant 조합)
    @Query("""
        SELECT tb FROM TimeBlock tb
        WHERE tb.meeting.id = :meetingId AND tb.participant.id = :participantId
    """)
    fun findByMeetingAndParticipant(
        @Param("meetingId") meetingId: Int,
        @Param("participantId") participantId: Int
    ): TimeBlock?

    //
    @Query("""
     SELECT 
        at.participant.guestName as participantName,  
        at.availableDateTime.date as date, 
        at.time as time
    FROM AvailableTime at
    WHERE at.meeting.id = :meetingId
    ORDER BY at.participant.guestName, at.availableDateTime.date, at.time 
""")
    fun findScheduleSlotsByMeetingId(@Param("meetingId") meetingId: Int): List<ParticipantScheduleSlot>

    @Query("""
    SELECT 
        at.time AS time,
        at.availableDateTime.date as date, 
        at.participant.id AS participantId,
        m.duration AS meetingDuration          
    FROM AvailableTime at
    JOIN at.meeting m
    WHERE at.meeting.id = :meetingId
    ORDER BY at.availableDateTime.date, at.time
""")
    fun findTimeSlotsWithMeta(@Param("meetingId") meetingId: Int): List<TimeSlotWithMetaProjection>
}