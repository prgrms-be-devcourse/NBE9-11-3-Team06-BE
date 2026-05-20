package com.back.nbe9112team06.domain.meeting.repository

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingRepository : JpaRepository<Meeting, Int> {
    fun existsByRandomUrl(randomUrl: String): Boolean
    fun findByRandomUrl(randomUrl: String): Meeting?
    fun findByMember_IdOrderByCreatedAtDesc(memberId: Int): List<Meeting>
}

