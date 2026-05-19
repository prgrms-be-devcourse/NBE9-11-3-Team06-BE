package com.back.nbe9112team06.domain.member.repository

import com.back.nbe9112team06.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Int> {

    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean

    fun findByEmail(email: String): Optional<Member>
    fun findByNickname(nickname: String): Optional<Member>
}