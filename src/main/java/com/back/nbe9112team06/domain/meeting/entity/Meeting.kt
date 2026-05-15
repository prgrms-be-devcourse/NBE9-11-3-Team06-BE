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
// TODO: Phase 2 - 주 생성자(primary constructor) 방식으로 재구성, var → val/private set, nullable 제거
class Meeting : BaseEntity() {
    var title: String? = null
    var category: String? = null

    @Column(name = "local_time")
    var localTime: String? = null

    @Enumerated(EnumType.STRING)
    var status: MeetingStatus? = null

    @Column(name = "random_url")
    var randomUrl: String? = null

    var duration: Int? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var member: Member? = null

    var confirmedDate: LocalDate? = null
    var confirmedTime: LocalTime? = null

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val participants: MutableList<Participant> = mutableListOf()

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val meetingsDates: MutableList<MeetingsDate> = mutableListOf()

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val timeBlocks: MutableList<TimeBlock> = mutableListOf()

    @OneToMany(mappedBy = "meeting", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    val timeTables: MutableList<TimeTable> = mutableListOf()

    fun confirm(date: LocalDate, time: LocalTime) {
        this.confirmedDate = date
        this.confirmedTime = time
        this.status = MeetingStatus.CONFIRMED
    }

    fun cancelConfirm() {
        this.confirmedDate = null
        this.confirmedTime = null
        this.status = MeetingStatus.PENDING
    }

    fun isHost(memberId: Int): Boolean {
        return this.member?.id == memberId
    }

    fun addMeetingsDate(meetingsDate: MeetingsDate) {
        meetingsDates.add(meetingsDate)
        meetingsDate.assignMeeting(this)
    }

    fun addParticipant(participant: Participant) {
        participants.add(participant)
        participant.assignMeeting(this)
    }

    companion object {
        // TODO: hase 2에서 create() 자체를 없애고 주 생성자로 대체
        @JvmStatic
        fun create(title: String, category: String, duration: Int, member: Member, randomUrl: String): Meeting {
            val meeting = Meeting()
            meeting.title = title
            meeting.category = category
            meeting.duration = duration
            meeting.member = member
            meeting.randomUrl = randomUrl
            meeting.status = MeetingStatus.PENDING
            return meeting
        }
    }
}
