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
class Meeting() : BaseEntity() {

    var title: String? = null
        protected set

    var category: String? = null
        protected set

    @Column(name = "local_time")
    var localTime: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    var status: MeetingStatus? = null
        protected set

    @Column(name = "random_url")
    var randomUrl: String? = null
        protected set

    var duration: Int? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    var member: Member? = null
        protected set

    var confirmedDate: LocalDate? = null
        protected set

    var confirmedTime: LocalTime? = null
        protected set

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

    fun isHost(memberId: Int): Boolean = member?.id == memberId

    fun addMeetingsDate(meetingsDate: MeetingsDate) {
        meetingsDates.add(meetingsDate)
        meetingsDate.assignMeeting(this)
    }

    fun addParticipant(participant: Participant) {
        participants.add(participant)
        participant.assignMeeting(this)
    }

    constructor(title: String, category: String, duration: Int, member: Member, randomUrl: String) : this() {
        this.title = title
        this.category = category
        this.duration = duration
        this.member = member
        this.randomUrl = randomUrl
        this.status = MeetingStatus.PENDING
    }
}
