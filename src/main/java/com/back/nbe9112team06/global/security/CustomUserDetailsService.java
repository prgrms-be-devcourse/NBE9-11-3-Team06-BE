package com.back.nbe9112team06.global.security;

import com.back.nbe9112team06.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * Spring Security 내부 연동용 인터페이스 구현체
     *    현재 구조(JWT + Stateless)에서는 직접 호출되지 않음
     *    AuthService.login()이 직접 검증하므로 이 메서드는 대기 상태
     *    향후 Spring Security AuthenticationManager 연동 시 활성화
     *     Spring Security 인터페이스 규약상 UsernameNotFoundException 유지
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // AuthService에서 이미 검증 완료된 후 호출되는 구조이므로
        return memberRepository.findByEmail(email)
                .map(member -> new SecurityUser(
                        member.getId(),
                        member.getNickname()
                ))
                .orElseThrow(() ->
                        // 단, 이 경로는 정상 흐름에서 도달하지 않음
                        new UsernameNotFoundException("도달하면 안 되는 경로: " + email)
                );
    }
}