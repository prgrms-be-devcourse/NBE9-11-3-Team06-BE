package com.back.nbe9112team06.domain.member.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Member() : BaseEntity() {

    @Column(nullable = false, unique = true)
    lateinit var email: String

    @Column(name = "password_hash", nullable = false)
    lateinit var passwordHash: String

    @Column(nullable = false)
    lateinit var nickname: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var timezone: TimezoneType

    @OneToMany(mappedBy = "member", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    var meetings: MutableList<Meeting> = mutableListOf()

    constructor(email: String, passwordHash: String, nickname: String, timezone: TimezoneType) : this() {
        this.email = email
        this.passwordHash = passwordHash
        this.nickname = nickname
        this.timezone = timezone
    }

    constructor(id: Int, nickname: String) : this() {
        this.id = id
        this.nickname = nickname
    }

    fun getName() = nickname
}