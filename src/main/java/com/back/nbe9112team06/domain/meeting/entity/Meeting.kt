package com.back.nbe9112team06.domain.meeting.entity

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.participant.entity.Participant
import com.back.nbe9112team06.domain.timeblock.entity.TimeBlock
import com.back.nbe9112team06.domain.timetable.entity.TimeTable
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

@Entity
// TODO: Phase 2 - 테스트 코드 Kotlin 변환 완료 후 @JvmOverloads 제거
class Meeting @JvmOverloads constructor(
    var title: String,
    var category: String,
    var duration: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var member: Member,

    @Column(name = "random_url")
    var randomUrl: String,

    @Enumerated(EnumType.STRING)
    var status: MeetingStatus = MeetingStatus.PENDING,

     //타임존 구현 시 필요
    @Column(name = "local_time")
    var localTime: String? = null,

    var confirmedDate: LocalDate? = null,
    var confirmedTime: LocalTime? = null,

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val participants: MutableList<Participant> = mutableListOf(),

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val meetingsDates: MutableList<MeetingsDate> = mutableListOf(),

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val timeBlocks: MutableList<TimeBlock> = mutableListOf(),

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val timeTables: MutableList<TimeTable> = mutableListOf()

) : BaseEntity() {

    fun confirm(date: LocalDate, time: LocalTime) {
        confirmedDate = date
        confirmedTime = time
        status = MeetingStatus.CONFIRMED
    }

    fun cancelConfirm() {
        confirmedDate = null
        confirmedTime = null
        status = MeetingStatus.PENDING
    }

    fun isHost(memberId: Int): Boolean = member.id == memberId

    fun addMeetingsDate(meetingsDate: MeetingsDate) {
        meetingsDates.add(meetingsDate)
        meetingsDate.assignMeeting(this)
    }

    fun addParticipant(participant: Participant) {
        participants.add(participant)
        participant.assignMeeting(this)
    }
}
