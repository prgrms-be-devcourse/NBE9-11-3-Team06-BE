package com.back.nbe9112team06.global.security

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.testutil.AuthTokenHelper
import com.back.nbe9112team06.testutil.MemberTestFactory
import io.jsonwebtoken.ExpiredJwtException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils

/**
 * JwtTokenProvider 단위 테스트
 * - Spring Context 필요 (@Value 주입 때문)
 * - DB 불필요 → @Transactional 없음
 * - JWT 생성 → 파싱 → 페이로드 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JwtTokenProvider 단위 테스트")
internal class JwtTokenProviderTest {
    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var authTokenHelper: AuthTokenHelper

    @Autowired
    private lateinit var memberTestFactory: MemberTestFactory

    // 테스트용 경량 Member (DB 저장 불필요)
    private fun testMember(): Member {
        val m = Member(
            "jwt-test@example.com",
            "hashedPw",
            "JWT테스터",
            TimezoneType.ASIA_SEOUL
        )
        // id 설정 (BaseEntity setId 필요 시 reflection 또는 생성자 활용)
        ReflectionTestUtils.setField(m, "id", 99)
        return m
    }

    @Nested
    @DisplayName("generateAccessToken - 토큰 생성")
    inner class GenerateAccessToken {
        @Test
        @DisplayName("Member로 토큰 생성 → 빈 문자열 아님")
        fun t1_generateToken_notBlank() {
            // when
            val token = jwtTokenProvider.generateAccessToken(testMember())

            // then
            assertThat(token).isNotBlank()
            println("accessToken = $token")
        }

        @Test
        @DisplayName("같은 Member로 두 번 생성 → 발급 시간 다르므로 토큰 다름")
        @Throws(InterruptedException::class)
        fun t2_sameMemeber_differentToken() {
            val m = testMember()
            val token1 = jwtTokenProvider.generateAccessToken(m)
            Thread.sleep(1000) // iat(발급시간) 차이 유발
            val token2 = jwtTokenProvider.generateAccessToken(m)

            assertThat(token1).isNotEqualTo(token2)
        }
    }

    @Nested
    @DisplayName("getPayload - 페이로드 추출")
    inner class GetPayload {
        @Test
        @DisplayName("생성한 토큰의 payload에 id 포함")
        fun t3_payload_containsId() {
            val m = testMember()
            val token = jwtTokenProvider.generateAccessToken(m)

            val payload = jwtTokenProvider.getPayload(token)

            assertThat(payload).isNotNull() // not null 확인했으므로 아래에 !!해야한다.
            assertThat(payload!!.get("id", Int::class.javaObjectType)).isEqualTo(m.id)
        }

        @Test
        @DisplayName("생성한 토큰의 payload에 nickname 포함")
        fun t4_payload_containsNickname() {
            val m = testMember()
            val token = jwtTokenProvider.generateAccessToken(m)

            val payload = jwtTokenProvider.getPayload(token)

            assertThat(payload).isNotNull()
            assertThat(payload!!.get("nickname", String::class.java)).isEqualTo("JWT테스터")
        }

        @Test
        @DisplayName("payload에 email 미포함 (민감정보 보호)")
        fun t5_payload_notContainEmail() {
            val token = jwtTokenProvider.generateAccessToken(testMember())
            val payload = jwtTokenProvider.getPayload(token)

            assertThat(payload).isNotNull()
            assertThat(payload!!.containsKey("email")).isFalse()
            assertThat(payload.containsKey("passwordHash")).isFalse()
        }

        @Test
        @DisplayName("위조 토큰 → null 반환")
        fun t6_tamperedToken_returnsNull() {
            val payload = jwtTokenProvider.getPayload("forged.token.value")
            assertThat(payload).isNull()
        }

        @Test
        @DisplayName("다른 secret으로 서명된 토큰 → null 반환")
        fun t7_differentSecretToken_returnsNull() {
            val m = testMember()
            val fakeToken = authTokenHelper.createForgedToken(
                m, "attacker-secret-completely-different!!"
            )

            val payload = jwtTokenProvider.getPayload(fakeToken)
            assertThat(payload).isNull()
        }

        @Test
        @DisplayName("만료 토큰 → ExpiredJwtException 발생")
        fun t8_expiredToken_throwsException() {
            val m = testMember()
            val expiredToken = authTokenHelper.createToken(m, -1L)

            assertThatThrownBy{ jwtTokenProvider.getPayload(expiredToken)}
                .isInstanceOf(ExpiredJwtException::class.java)
        }
    }
}
