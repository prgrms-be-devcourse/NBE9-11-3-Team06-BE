package com.back.nbe9112team06.domain.auth.service

import com.back.nbe9112team06.domain.auth.dto.LoginRequest
import com.back.nbe9112team06.domain.auth.dto.LoginResult
import com.back.nbe9112team06.domain.member.service.MemberService
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import com.back.nbe9112team06.global.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val memberService: MemberService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun login(request: LoginRequest): LoginResult {
        val member = memberService.findByEmail(request.email) ?:
        throw BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS)

        if (!passwordEncoder.matches(request.password, member.passwordHash)) {
            throw BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS)
        }

        val accessToken = jwtTokenProvider.generateAccessToken(member)

        return LoginResult(
            accessToken,
            member.id,
            member.nickname
        )
    }
}
