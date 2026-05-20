package com.back.nbe9112team06.domain.member.repository

import com.back.nbe9112team06.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Int> {

    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean

    fun findByEmail(email: String): Member?
    fun findByNickname(nickname: String): Member?
}