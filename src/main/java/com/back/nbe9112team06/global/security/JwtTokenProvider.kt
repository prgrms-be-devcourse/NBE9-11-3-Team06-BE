package com.back.nbe9112team06.global.security

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.standard.ut.Ut
import io.jsonwebtoken.Claims
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.access-token-expire-seconds:3600}")
    private val expireSeconds: Long
) {
    //TODO member entity가 변환전이라서 임시로 getName()으로 접근(private이기떄문에)
    fun generateAccessToken(member: Member): String {
        val body = mapOf(
            "id" to member.id,
            "nickname" to member.getName()
        )
        return Ut.toString(secret, expireSeconds, body)
    }

    // Claims 타입으로 반환 → payload.get("id", Integer.class) 사용 가능
    fun getPayload(token: String): Claims? {
        return Ut.payloadOrNull(token, secret)
    }
}