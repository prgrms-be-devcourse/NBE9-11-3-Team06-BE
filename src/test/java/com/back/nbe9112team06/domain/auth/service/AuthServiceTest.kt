package com.back.nbe9112team06.domain.auth.service

import com.back.nbe9112team06.domain.auth.dto.LoginRequest
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import com.back.nbe9112team06.global.exception.BusinessException
import com.back.nbe9112team06.global.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * AuthService 단위 테스트
 * - 로그인 성공/실패 비즈니스 로직 검증
 * - 토큰 생성 결과 검증
 * - HTTP 레이어 없음 (Cookie, MockMvc 불필요)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {
    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var savedMember: Member

    @BeforeEach
    fun setUp() {
        savedMember = memberRepository.save(
            Member(
                TEST_EMAIL,
                passwordEncoder.encode(TEST_PASSWORD)!!,
                TEST_NICKNAME,
                TimezoneType.ASIA_SEOUL
            )
        )
    }

    @Test
    @DisplayName("t1: AuthService Bean 정상 주입")
    fun t1_beanNotNull() {
        assertThat(authService).isNotNull()
    }

    @Nested
    @DisplayName("login - 로그인 성공")
    inner class LoginSuccess {
        @Test
        @DisplayName("t2: 정상 로그인 → LoginResult 반환, accessToken 비어있지 않음")
        fun t2_login_success_returnsLoginResult() {
            // when
            val result = authService.login(
                LoginRequest(TEST_EMAIL, TEST_PASSWORD)
            )

            // then
            assertThat(result).isNotNull()
            assertThat(result.accessToken).isNotBlank()
            assertThat(result.memberId).isEqualTo(savedMember.id)
            assertThat(result.nickname).isEqualTo(TEST_NICKNAME)

            println("accessToken = " + result.accessToken)
        }

        @Test
        @DisplayName("t3: 로그인 성공 시 반환된 accessToken → payload에 id, nickname 포함")
        fun t3_login_token_containsPayload() {
            // when
            val result = authService.login(
                LoginRequest(TEST_EMAIL, TEST_PASSWORD)
            )

            // then: 토큰 파싱하여 페이로드 검증
            val payload = jwtTokenProvider.getPayload(result.accessToken)
            assertThat(payload).isNotNull()
            assertThat(payload!!.get("id", Int::class.javaObjectType))
                .isEqualTo(savedMember.id)
            assertThat(payload.get("nickname", String::class.java))
                .isEqualTo(TEST_NICKNAME)
        }

        @Test
        @DisplayName("t4: 로그인 성공 시 accessToken payload에 email 미포함 (민감정보 보호)")
        fun t4_login_token_noEmailInPayload() {
            val result = authService.login(
                LoginRequest(TEST_EMAIL, TEST_PASSWORD)
            )

            val payload = jwtTokenProvider.getPayload(result.accessToken)
            assertThat(payload).isNotNull()
            assertThat(payload!!.containsKey("email")).isFalse()
            assertThat(payload.containsKey("passwordHash")).isFalse()
        }
    }

    @Nested
    @DisplayName("login - 로그인 실패")
    internal inner class LoginFail {
        @Test
        @DisplayName("t5: 비밀번호 오류 → BusinessException(AUTH-004)")
        fun t5_wrongPassword_throwsBusinessException() {
            assertThatThrownBy {
                authService.login(
                    LoginRequest(
                        TEST_EMAIL,
                        "wrongPassword!"
                    )
                )
            }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex: Throwable ->
                    val be = ex as BusinessException
                    assertThat(be.getErrorCode()).isEqualTo("AUTH-004")
                })
        }

        @Test
        @DisplayName("t6: 존재하지 않는 이메일 → BusinessException(AUTH-004)")
        fun t6_notExistEmail_throwsBusinessException() {
            assertThatThrownBy {
                authService.login(
                    LoginRequest(
                        "ghost@example.com",
                        TEST_PASSWORD
                    )
                )
            }
                .isInstanceOf(BusinessException::class.java)
                .satisfies({ ex: Throwable ->
                    val be = ex as BusinessException
                    assertThat(be.getErrorCode()).isEqualTo("AUTH-004")
                })
        }

        @Test
        @DisplayName("t7: 이메일 없음/비밀번호 오류 → 동일한 에러코드 (보안: 구분 안 함)")
        fun t7_sameErrorCode_for_emailAndPasswordError() {
            // 보안 원칙: 어느 쪽이 틀렸는지 노출하지 않음
            var codeForWrongPassword: String? = null
            var codeForWrongEmail: String? = null

            try {
                authService.login(LoginRequest(TEST_EMAIL, "wrongPassword!"))
            } catch (e: BusinessException) {
                codeForWrongPassword = e.getErrorCode()
            }

            try {
                authService.login(LoginRequest("ghost@example.com", TEST_PASSWORD))
            } catch (e: BusinessException) {
                codeForWrongEmail = e.getErrorCode()
            }

            assertThat(codeForWrongPassword).isEqualTo(codeForWrongEmail)
        }
    }

    companion object {
        private const val TEST_EMAIL = "auth-service@example.com"
        private const val TEST_PASSWORD = "password123!"
        private const val TEST_NICKNAME = "서비스테스터"
    }
}