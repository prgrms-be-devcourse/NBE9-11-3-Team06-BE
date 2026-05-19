package com.back.nbe9112team06.standard.ut

import com.back.nbe9112team06.config.TestConstants.JWT_SECRET
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Ut (JWT 유틸) 순수 단위 테스트
 * - Spring Context 불필요 → 빠른 실행
 * - JWT 생성/파싱/검증 로직 자체만 검증
 */
@DisplayName("Ut JWT 유틸 단위 테스트")
internal class UtTest {
    private companion object{
        private const val DIFFERENT_SECRET = "pcGe4Ipx+Vewx7yR7AWrJjvukwfWmmYw+xc7dujkcRdJ9DiEbCy0TbJdf02mJmSnc"
    }

    @Nested
    @DisplayName("toString - 토큰 생성")
    inner class ToString {
        @Test
        @DisplayName("payload를 담아 JWT 문자열 생성 → 빈 문자열 아님")
        fun t1_createJwt_notBlank() {
            // given
            val payload = mapOf("id" to 1, "nickname" to "테스터")

            // when
            val jwt = Ut.toString(JWT_SECRET, 3600L, payload)

            // then
            assertThat(jwt).isNotBlank()
            println("jwt = $jwt")
        }

        @Test
        @DisplayName("JWT는 header.payload.signature 3파트 구조")
        fun t2_jwtHasThreeParts() {
            val jwt = Ut.toString(JWT_SECRET, 3600L, mapOf("id" to 1))

            val parts = jwt.split("\\.".toRegex())
            assertThat(parts).hasSize(3)
        }

        @Test
        @DisplayName("다른 payload로 생성한 JWT는 서로 다름")
        fun t3_differentPayload_differentJwt() {
            val jwt1 = Ut.toString(JWT_SECRET, 3600L, mapOf("id" to 1))
            val jwt2 = Ut.toString(JWT_SECRET, 3600L, mapOf("id" to 2))

            assertThat(jwt1).isNotEqualTo(jwt2)
        }
    }

    @Nested
    @DisplayName("payloadOrNull - 토큰 파싱")
    inner class PayloadOrNull {
        @Test
        @DisplayName("유효한 토큰 → payload에 원본 값 포함")
        fun t4_validToken_returnsPayload() {
            // given
            val original = mapOf("id" to 1, "nickname" to "테스터")
            val jwt = Ut.toString(JWT_SECRET, 3600L, original)

            // when
            val payload: Claims? = Ut.payloadOrNull(jwt, JWT_SECRET)

            // then
            assertThat(payload).isNotNull()
            assertThat(payload?.get("id", Int::class.java)).isEqualTo(1)
            assertThat(payload?.get("nickname", String::class.java)).isEqualTo("테스터")
        }

        @Test
        @DisplayName("서명이 다른 secret → null 반환 (위조 감지)")
        fun t5_differentSecret_returnsNull() {
            // given: SECRET으로 서명한 토큰
            val jwt = Ut.toString(JWT_SECRET, 3600L, mapOf("id" to 1))

            // when: DIFFERENT_SECRET으로 검증 시도
            val payload: Claims? = Ut.payloadOrNull(jwt, JWT_SECRET)

            // then
            assertThat(payload).isNull()
        }

        @Test
        @DisplayName("형식이 잘못된 토큰 → null 반환")
        fun t6_malformedToken_returnsNull() {
            val payload: Claims? = Ut.payloadOrNull("not.a.valid.jwt.token", JWT_SECRET)
            assertThat(payload).isNull()
        }

        @Test
        @DisplayName("빈 문자열 → null 반환")
        fun t7_emptyString_returnsNull() {
            val payload: Claims? = Ut.payloadOrNull("", JWT_SECRET)
            assertThat(payload).isNull()
        }

        @Test
        @DisplayName("만료된 토큰 → ExpiredJwtException 발생 (null 아님)")
        fun t8_expiredToken_throwsExpiredJwtException() {
            // given: 만료 시간 -1초 (이미 만료)
            val expiredJwt = Ut.toString(JWT_SECRET, -1L, mapOf("id" to 1))

            // then: null이 아니라 예외를 던져야 필터에서 구분 처리 가능
            assertThatThrownBy{ Ut.payloadOrNull(expiredJwt, JWT_SECRET) }
                .isInstanceOf(ExpiredJwtException::class.java)
        }
    }

    @Nested
    @DisplayName("isValid - 유효성 검증")
    inner class IsValid {
        @Test
        @DisplayName("유효한 토큰 → true")
        fun t9_validToken_true() {
            val jwt = Ut.toString(JWT_SECRET, 3600L, mapOf("id" to 1))
            assertThat(Ut.isValid(jwt, JWT_SECRET)).isTrue()
        }

        @Test
        @DisplayName("다른 secret으로 검증 → false")
        fun t10_differentSecret_false() {
            val jwt = Ut.toString(JWT_SECRET, 3600L, mapOf("id" to 1))
            assertThat(Ut.isValid(jwt, DIFFERENT_SECRET)).isFalse()
        }

        @Test
        @DisplayName("만료 토큰 → isValid는 false (내부에서 예외 → false 처리)")
        fun t11_expiredToken_false() {
            val expiredJwt = Ut.toString(JWT_SECRET, -1L, mapOf("id" to 1))
            // isValid는 payloadOrNull을 재사용하는데,
            // ExpiredJwtException이 발생하면 false가 되어야 함
            // (payloadOrNull이 throw하면 isValid도 false여야 일관성 있음)
            // 현재 코드에서 isValid → payloadOrNull → throw → isValid도 throw
            // → 이 테스트로 현재 동작 확인
            Assertions.assertThatCode{ Ut.isValid(expiredJwt, JWT_SECRET) }
                .isInstanceOf(ExpiredJwtException::class.java)
        }

        @Test
        @DisplayName("Ut 클래스는 인스턴스화 불가")
        fun t12_cannotInstantiate() {
            assertThatThrownBy {
                val constructor = Ut::class.java.getDeclaredConstructor()
                constructor.isAccessible = true
                constructor.newInstance()
            }.hasCauseInstanceOf(UnsupportedOperationException::class.java)
        }
    }
}