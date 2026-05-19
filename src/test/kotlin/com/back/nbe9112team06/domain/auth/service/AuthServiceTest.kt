//package com.back.nbe9112team06.domain.auth.service
//
//import com.back.nbe9112team06.domain.auth.dto.LoginRequest
//import com.back.nbe9112team06.domain.member.entity.TimezoneType
//import com.back.nbe9112team06.domain.member.service.MemberService
//import com.back.nbe9112team06.global.error.ErrorCode
//import com.back.nbe9112team06.global.exception.BusinessException
//import com.back.nbe9112team06.global.security.JwtTokenProvider
//import io.mockk.every
//import io.mockk.junit5.MockKExtension
//import io.mockk.mockk
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.ExtendWith
//import org.springframework.security.crypto.password.PasswordEncoder
//import java.util.Optional
//
//@ExtendWith(MockKExtension::class)
//@DisplayName("AuthService.login() 단위 테스트")
//class AuthServiceTest {
//
//    // 🔹 의존성 모킹
//    private val memberService = mockk<MemberService>()
//    private val jwtTokenProvider = mockk<JwtTokenProvider>()
//    private val passwordEncoder = mockk<PasswordEncoder>()
//
//    // 🔹 테스트 대상 (수동 주입)
//    private lateinit var authService: AuthService
//
//    private companion object {
//        private const val TEST_EMAIL = "test@example.com"
//        private const val TEST_PASSWORD = "password123!"
//        private const val TEST_NICKNAME = "테스터"
//        private const val TEST_MEMBER_ID = 1L
//        private const val MOCK_TOKEN = "mock.jwt.token"
//    }
//
//    @BeforeEach
//    fun setUp() {
//        authService = AuthService(memberService, jwtTokenProvider, passwordEncoder)
//    }
//
//    // ─────────────────────────────────────────
//    // ✅ 성공 시나리오
//    // ─────────────────────────────────────────
//    @Nested
//    @DisplayName("성공: 정상 로그인")
//    inner class Success {
//
//        @Test
//        fun `존재하는 이메일 + 올바른 비밀번호 → LoginResult 반환`() {
//            // given: 테스트 데이터
//            val member = createTestMember(id = TEST_MEMBER_ID, email = TEST_EMAIL)
//
//            // given: 모킹 설정
//            every { memberService.findByEmail(TEST_EMAIL) } returns Optional.of(member)
//            every { passwordEncoder.matches(TEST_PASSWORD, member.passwordHash) } returns true
//            every { jwtTokenProvider.generateAccessToken(member) } returns MOCK_TOKEN
//
//            // when
//            val result = authService.login(LoginRequest(TEST_EMAIL, TEST_PASSWORD))
//
//            // then: 결과 검증
//            assertThat(result).isNotNull()
//            assertThat(result.accessToken).isEqualTo(MOCK_TOKEN)
//            assertThat(result.memberId).isEqualTo(TEST_MEMBER_ID)
//            assertThat(result.nickname).isEqualTo(TEST_NICKNAME)
//
//            // then: 의존성 호출 검증
//            verify(exactly = 1) { memberService.findByEmail(TEST_EMAIL) }
//            verify(exactly = 1) { passwordEncoder.matches(TEST_PASSWORD, member.passwordHash) }
//            verify(exactly = 1) { jwtTokenProvider.generateAccessToken(member) }
//        }
//    }
//
//    // ─────────────────────────────────────────
//    // ❌ 실패 시나리오
//    // ─────────────────────────────────────────
//    @Nested
//    @DisplayName("실패: 로그인 오류")
//    inner class Failure {
//
//        @Test
//        fun `존재하지 않는 이메일 → BusinessException 발생`() {
//            // given
//            every { memberService.findByEmail("ghost@example.com") } returns Optional.empty()
//
//            // when & then
//            assertThatThrownBy {
//                authService.login(LoginRequest("ghost@example.com", TEST_PASSWORD))
//            }
//                .isInstanceOf(BusinessException::class.java)
//                .extracting("errorCode")
//                .isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS.code)
//
//            // then: 비밀번호 검증, 토큰 발급은 절대 호출되지 않아야 함
//            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
//            verify(exactly = 0) { jwtTokenProvider.generateAccessToken(any()) }
//        }
//
//        @Test
//        fun `비밀번호 불일치 → BusinessException 발생`() {
//            // given
//            val member = createTestMember(email = TEST_EMAIL)
//            every { memberService.findByEmail(TEST_EMAIL) } returns Optional.of(member)
//            every { passwordEncoder.matches("wrong!", member.passwordHash) } returns false
//
//            // when & then
//            assertThatThrownBy {
//                authService.login(LoginRequest(TEST_EMAIL, "wrong!"))
//            }
//                .isInstanceOf(BusinessException::class.java)
//                .extracting("errorCode")
//                .isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS.code)
//
//            // then: 토큰 발급은 호출되지 않아야 함 (실패 시 조기 리턴)
//            verify(exactly = 0) { jwtTokenProvider.generateAccessToken(any()) }
//        }
//
//        @Test
//        fun `이메일 없음과 비밀번호 오류는 동일한 에러코드 반환 (보안)`() {
//            // given
//            val member = createTestMember(email = TEST_EMAIL)
//            every { memberService.findByEmail(TEST_EMAIL) } returns Optional.of(member)
//            every { passwordEncoder.matches(any(), any()) } returns false
//            every { memberService.findByEmail("ghost@example.com") } returns Optional.empty()
//
//            // when: 두 경우의 에러코드 추출
//            val codeForWrongEmail = runCatching {
//                authService.login(LoginRequest("ghost@example.com", TEST_PASSWORD))
//            }.exceptionOrNull()?.let { (it as BusinessException).errorCode }
//
//            val codeForWrongPassword = runCatching {
//                authService.login(LoginRequest(TEST_EMAIL, "wrong!"))
//            }.exceptionOrNull()?.let { (it as BusinessException).errorCode }
//
//            // then: 두 에러코드가 동일해야 함 (구체적 실패 원인 노출 금지)
//            assertThat(codeForWrongEmail).isEqualTo(codeForWrongPassword)
//            assertThat(codeForWrongEmail).isEqualTo(ErrorCode.INVALID_LOGIN_CREDENTIALS.code)
//        }
//    }
//
//    // ─────────────────────────────────────────
//    // 🔧 헬퍼 함수 (테스트 데이터 빌더)
//    // ─────────────────────────────────────────
//    private fun createTestMember(
//        id: Long = TEST_MEMBER_ID,
//        email: String = TEST_EMAIL,
//        passwordHash: String = "encoded_hash",
//        nickname: String = TEST_NICKNAME
//    ) = Member(
//        id = id,
//        email = email,
//        passwordHash = passwordHash,
//        nickname = nickname,
//        timezone = TimezoneType.ASIA_SEOUL
//        // 필요시 다른 필드 추가
//    )
//}