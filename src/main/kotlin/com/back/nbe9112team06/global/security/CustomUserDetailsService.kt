package com.back.nbe9112team06.global.security

import com.back.nbe9112team06.domain.member.repository.MemberRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {
    /**
     * Spring Security 내부 연동용 인터페이스 구현체
     * 현재 구조(JWT + Stateless)에서는 직접 호출되지 않음
     * AuthService.login()이 직접 검증하므로 이 메서드는 대기 상태
     * 향후 Spring Security AuthenticationManager 연동 시 활성화
     * Spring Security 인터페이스 규약상 UsernameNotFoundException 유지
     */
    //TODO Member 가 아직 java라서 프로퍼티처럼 접근불가
    override fun loadUserByUsername(email: String): UserDetails {
        // AuthService에서 이미 검증 완료된 후 호출되는 구조이므로
        return memberRepository.findByEmail(email)
            ?.let{ member ->
                SecurityUser(
                    id = member.id,
                    nickname = member.getName()
                )
            }
            ?: throw UsernameNotFoundException("도달하면 안 되는 경로: $email")
    }
}