package com.back.nbe9112team06.testutil

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*

@Component
class MemberTestFactory(private val passwordEncoder: PasswordEncoder) {
    /**
     * 이메일을 지정하여 테스트용 Member 생성
     */
    /**
     * 기본 테스트용 Member 생성 (DB 저장 전 상태)
     */
    fun createMember(email: String = "user-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com"): Member {
        return Member(
            email,
            passwordEncoder.encode("password123!")!!,  // 기본 비밀번호
            "테스터-" + UUID.randomUUID().toString().substring(0, 6),
            TimezoneType.ASIA_SEOUL
        )
    }

    /**
     * 모든 필드를 커스터마이징하여 생성
     */
    fun createMember(email: String, password: String, nickname: String, timezone: TimezoneType): Member {
        return Member(
            email,
            passwordEncoder.encode(password)!!,
            nickname,
            timezone
        )
    }

    /**
     * DB 에 저장까지 완료된 Member 반환 (편의 메서드)
     */
    fun createAndSaveMember(memberRepository: MemberRepository): Member {
        val member = createMember()
        return memberRepository.save<Member>(member)
    }

    fun createAndSaveMember(memberRepository: MemberRepository, email: String): Member {
        val member = createMember(email)
        return memberRepository.save<Member>(member)
    }

    fun createAndSaveMember(
        memberRepository: MemberRepository,
        email: String, password: String, nickname: String
    ): Member {
        val member = createMember(email, password, nickname, TimezoneType.ASIA_SEOUL)
        return memberRepository.save<Member>(member)
    } //    public Member createAndSaveMember(MemberRepository repository,
    //                                      String email, String password,
    //                                      String nickname, TimezoneType timezone) {
    //        Member member = createMember(email, password, nickname, timezone);
    //        return repository.save(member);
    //    }
}