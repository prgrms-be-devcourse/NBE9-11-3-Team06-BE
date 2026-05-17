package com.back.nbe9112team06.domain.member.service

import com.back.nbe9112team06.domain.member.dto.SignupRequest
import com.back.nbe9112team06.domain.member.dto.request.CheckEmailRequest
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    // 회원가입
    @Transactional
    fun signup(request: SignupRequest): Member {
        if (memberRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }

        val hashedPassword: String = passwordEncoder.encode(request.password)!!

        val member = Member(
            request.email,
            hashedPassword,
            request.nickname,
            request.timezone,
        )
        return memberRepository.save(member)
    }

    // 회원 탈퇴
    @Transactional
    fun deleteMember(memberId: Int) {
        val member = memberRepository.findById(memberId)
            .orElseThrow { BusinessException(ErrorCode.NOT_FOUND) }

        memberRepository.delete(member)
    }

    // 이메일 중복 체크
    fun checkEmail(request: CheckEmailRequest): Boolean =
        memberRepository.existsByEmail(request.email)

    // 조회 메서드 (Optional 유지 - Java 호출 측 호환)
    fun findById(memberId: Int): Optional<Member> = memberRepository.findById(memberId)

    fun findByEmail(email: String): Optional<Member> = memberRepository.findByEmail(email)
}