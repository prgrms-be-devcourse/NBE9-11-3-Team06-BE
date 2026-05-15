package com.back.nbe9112team06.domain.member.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Member() : BaseEntity() {

    @Column(nullable = false, unique = true)
    var email: String? = null
        protected set

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String? = null
        protected set

    @Column(nullable = false)
    var nickname: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var timezone: TimezoneType? = null
        protected set

    @OneToMany(mappedBy = "member", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    var meetings: MutableList<Meeting> = mutableListOf()
        protected set

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
