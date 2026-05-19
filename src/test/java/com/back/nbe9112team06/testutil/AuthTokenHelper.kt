package com.back.nbe9112team06.testutil

import com.back.nbe9112team06.config.TestConstants
import com.back.nbe9112team06.config.TestConstants.DEFAULT_EXPIRE_SECONDS
import com.back.nbe9112team06.config.TestConstants.JWT_SECRET
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.standard.ut.Ut
import org.springframework.stereotype.Component

@Component // ✅ 테스트 컨텍스트에서 주입 가능
class AuthTokenHelper {
    /**
     * 테스트용 JWT 토큰 생성
     * @param member 토큰에 포함할 사용자 정보
     * @param expireSeconds 만료 시간 (초). 음수면 즉시 만료
     * @param extraPayload 추가 클레임 (옵션)
     */

    fun createToken(
        member: Member,
        expireSeconds: Long = DEFAULT_EXPIRE_SECONDS,
        extraPayload: Map<String, Any> = emptyMap()
    ): String {
        // ✅ 원본 payload 를 변경하지 않도록 mutable copy 생성
        val payload = extraPayload.toMutableMap()
        payload["id"] = member.id
        payload["nickname"] = member.nickname

        return Ut.toString(JWT_SECRET, expireSeconds, payload)
    }

    // 오버로드: 기본 만료 시간 사용
    fun createToken(member: Member, extraPayload: Map<String, Any>): String {
        return createToken(member, DEFAULT_EXPIRE_SECONDS, extraPayload)
    }

    /**
     * 위조 토큰 생성 (다른 secret 으로 서명)
     */
    fun createForgedToken(member: Member, fakeSecret: String): String {
        return Ut.toString(
            fakeSecret, DEFAULT_EXPIRE_SECONDS,
            mapOf("id" to  member.id, "nickname" to  member.nickname)
        )
    }

    /**
     * 커스텀 payload 로 테스트용 토큰 생성
     * - 존재하지 않는 회원 id 등 특수 시나리오 테스트용
     */
    fun createTokenWithPayload(customPayload: Map<String, Any>): String {
        return Ut.toString(
            TestConstants.JWT_SECRET,
            TestConstants.DEFAULT_EXPIRE_SECONDS,
            customPayload
        )
    }
}