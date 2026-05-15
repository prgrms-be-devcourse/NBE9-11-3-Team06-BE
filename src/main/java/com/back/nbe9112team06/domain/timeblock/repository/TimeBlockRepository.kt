package com.back.nbe9112team06.domain.timeblock.repository

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TimeBlockRepository : JpaRepository<TimeBlock, Int> {

    // 중복 등록 체크 (같은 모임에 같은 참여자로 이미 등록했는지)
    fun findByMeetingAndParticipant(meeting: Meeting, participant: Participant): TimeBlock?

    // meetingId 로 모든 TimeBlock + 연관관계 fetch join 으로 한 번에 로드
    @Query(
        """
        select distinct tb from TimeBlock tb
        join fetch tb.participant p
        join fetch tb.availableDateTimes d
        where tb.meeting.id = :meetingId
        """
    )
    fun findWithAll(meetingId: Int): List<TimeBlock>
}