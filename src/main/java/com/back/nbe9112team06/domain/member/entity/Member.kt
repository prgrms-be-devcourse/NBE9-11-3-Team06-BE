package com.back.nbe9112team06.domain.member.entity

import com.back.nbe9112team06.domain.meeting.entity.Meeting
import com.back.nbe9112team06.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
class Member @JvmOverloads constructor(
    @Column(nullable = false, unique = true)
    var email: String,
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    @Column(nullable = false)
    var nickname: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var timezone: TimezoneType,
    @OneToMany(mappedBy = "member", cascade = [CascadeType.PERSIST, CascadeType.REMOVE], orphanRemoval = true)
    var meetings: MutableList<Meeting> = mutableListOf()
) : BaseEntity() {

    constructor(id: Int, nickname: String) : this(
        email = "",
        passwordHash = "",
        nickname = nickname,
        timezone = TimezoneType.ASIA_SEOUL
    ) {
        this.id = id
    }

    fun getName() = nickname
}
