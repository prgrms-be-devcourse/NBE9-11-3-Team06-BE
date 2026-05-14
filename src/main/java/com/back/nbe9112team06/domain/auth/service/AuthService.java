package com.back.nbe9112team06.domain.auth.service;

import com.back.nbe9112team06.domain.auth.dto.LoginRequest;
import com.back.nbe9112team06.domain.auth.dto.LoginResult;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.service.MemberService;
import com.back.nbe9112team06.global.error.ErrorCode;
import com.back.nbe9112team06.global.exception.BusinessException;
import com.back.nbe9112team06.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResult login(LoginRequest request) {
        Member member = memberService.findByEmail(request.email())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS)
                );

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(member);

        return new LoginResult(
                accessToken,
                member.getId(),
                member.getNickname()
        );
    }

}
