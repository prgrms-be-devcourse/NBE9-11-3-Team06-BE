package com.back.nbe9112team06.domain.auth.controller

import com.back.nbe9112team06.domain.auth.dto.LoginRequest
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import com.back.nbe9112team06.testutil.AuthTokenHelper
import com.back.nbe9112team06.testutil.MemberTestFactory
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
internal class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var authTokenHelper: AuthTokenHelper

    @Autowired
    private lateinit var memberTestFactory: MemberTestFactory

    private lateinit var savedMember: Member

    @BeforeEach
    fun setUp() {
        savedMember = memberTestFactory.createAndSaveMember(
            memberRepository, TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME
        )
    }

    @Nested
    @DisplayName("POST /api/auth/login - 로그인")
    internal inner class Login {
        @Test
        @DisplayName("t1: 정상 로그인 → 200, accessToken Cookie 발급")
        @Throws(Exception::class)
        fun t1_login_success_issuesCookie() {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, TEST_PASSWORD))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()

            val cookie = result.response.getCookie("accessToken")
            Assertions.assertThat<Cookie?>(cookie).isNotNull()
            Assertions.assertThat(cookie!!.value).isNotBlank()
            Assertions.assertThat(cookie.isHttpOnly).isTrue()
        }

        @Test
        @DisplayName("t2: 정상 로그인 → Response body에 accessToken 미포함")
        @Throws(Exception::class)
        fun t2_login_success_tokenNotInBody() {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()

            val body = result.response.contentAsString
            Assertions.assertThat(body).doesNotContain("accessToken")
            Assertions.assertThat(body).doesNotContain("eyJ")
        }

        @Test
        @DisplayName("t3: 정상 로그인 → Response body에 memberId, nickname 포함")
        @Throws(Exception::class)
        fun t3_login_success_bodyContainsMemberInfo() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(TEST_NICKNAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.passwordHash").doesNotExist())
        }

        @Test
        @DisplayName("t4: 정상 로그인 → resultCode 확인")
        @Throws(Exception::class)
        fun t4_login_success_resultCode() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그인 성공"))
        }

        @Test
        @DisplayName("t5: 비밀번호 오류 → 401, errorCode=AUTH-004, RFC 9457 형식")
        @Throws(Exception::class)
        fun t5_wrongPassword_401_auth004() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, "wrongPassword!"))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.content().contentType("application/problem+json"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-004"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(
                    MockMvcResultMatchers.jsonPath("$.detail")
                        .value("이메일 또는 비밀번호가 올바르지 않습니다.")
                )
        }

        @Test
        @DisplayName("t6: 존재하지 않는 이메일 → 401, errorCode=AUTH-004")
        @Throws(Exception::class)
        fun t6_notExistEmail_401_auth004() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("ghost@example.com", TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-004"))
        }

        @Test
        @DisplayName("t7: 이메일 형식 오류 → 400, errorCode=COMMON-009")
        @Throws(Exception::class)
        fun t7_invalidEmailFormat_400() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("not-an-email", TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-009"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isArray())
        }

        @Test
        @DisplayName("t8: 비밀번호 누락 → 400, errorCode=COMMON-002")
        @Throws(Exception::class)
        fun t8_missingPassword_400() {
            val body = jsonMapper.writeValueAsString(
                mapOf("email" to TEST_EMAIL)
            )

            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-002"))
        }

        @Test
        @DisplayName("t9: Content-Type 없음 → 415 Unsupported Media Type")
        @Throws(Exception::class)
        fun t9_noContentType_415() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .content(loginBody(TEST_EMAIL, TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me - 내 정보 조회")
    internal inner class GetMyInfo {
        @Test
        @DisplayName("t10: 유효한 토큰으로 조회 → 200, id+nickname 반환")
        @Throws(Exception::class)
        fun t10_validToken_200_memberInfo() {
            // ✅ AuthTokenHelper 활용: 토큰 생성 로직 일관화
            val token = authTokenHelper.createToken(savedMember)

            mockMvc.perform(
                MockMvcRequestBuilders.get(ME_URL)
                    .cookie(Cookie("accessToken", token))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(TEST_NICKNAME))
        }

        @Test
        @DisplayName("t11: 토큰 없음 → 401, errorCode=AUTH-001 (TOKEN_MISSING)")
        @Throws(Exception::class)
        fun t11_noToken_401_auth001() {
            mockMvc.perform(MockMvcRequestBuilders.get(ME_URL))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.content().contentType("application/problem+json;charset=UTF-8"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-001"))
        }

        @Test
        @DisplayName("t12: 위조된 토큰 → 401, errorCode=AUTH-002 (TOKEN_INVALID)")
        @Throws(Exception::class)
        fun t12_tamperedToken_401_auth002() {
            // ✅ AuthTokenHelper.createForgedToken() 활용: 위조 토큰 생성 명확화
            val forgedToken = authTokenHelper.createForgedToken(
                savedMember, "fake-secret-key-for-testing-purpose"
            )

            mockMvc.perform(
                MockMvcRequestBuilders.get(ME_URL)
                    .cookie(Cookie("accessToken", forgedToken))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-002"))
        }

        @Test
        @DisplayName("t13: 만료된 토큰 → 401, errorCode=AUTH-003 (TOKEN_EXPIRED)")
        @Throws(Exception::class)
        fun t13_expiredToken_401_auth003() {
            // ✅ AuthTokenHelper 활용: expireSeconds=-1L 로 즉시 만료 토큰 생성
            val expiredToken = authTokenHelper.createToken(savedMember, -1L)

            mockMvc.perform(
                MockMvcRequestBuilders.get(ME_URL)
                    .cookie(Cookie("accessToken", expiredToken))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-003"))
        }

        @Test
        @DisplayName("t14: 로그인 후 발급된 Cookie 로 내 정보 조회 → 연동 흐름 확인")
        @Throws(Exception::class)
        fun t14_loginThenMe_linkedFlow() {
            val loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, TEST_PASSWORD))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()

            val cookie = loginResult.response.getCookie("accessToken")
            Assertions.assertThat<Cookie?>(cookie).isNotNull()

            mockMvc.perform(
                MockMvcRequestBuilders.get(ME_URL)
                    .cookie(cookie!!)
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(TEST_NICKNAME))
        }

        @Test
        @DisplayName("t15: 응답 body 에 민감정보 미포함 확인")
        @Throws(Exception::class)
        fun t15_me_noSensitiveInfo() {
            val token = authTokenHelper.createToken(savedMember)

            val result = mockMvc.perform(
                MockMvcRequestBuilders.get(ME_URL)
                    .cookie(Cookie("accessToken", token))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()

            val body = result.response.contentAsString
            Assertions.assertThat(body).doesNotContain("passwordHash")
            Assertions.assertThat(body).doesNotContain(TEST_EMAIL)
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout - 로그아웃")
    internal inner class Logout {
        @Test
        @DisplayName("t16: 인증 후 로그아웃 → 200, Cookie maxAge=0")
        @Throws(Exception::class)
        fun t16_logout_success_cookieExpired() {
            val token = authTokenHelper.createToken(savedMember)

            val result = mockMvc.perform(
                MockMvcRequestBuilders.post(LOGOUT_URL)
                    .cookie(Cookie("accessToken", token))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그아웃 성공"))
                .andReturn()

            val clearedCookie = result.response.getCookie("accessToken")
            Assertions.assertThat<Cookie?>(clearedCookie).isNotNull()
            Assertions.assertThat(clearedCookie!!.maxAge).isEqualTo(0)
        }

        @Test
        @DisplayName("t17: 토큰 없이 로그아웃 → 401 (인증 필요 엔드포인트)")
        @Throws(Exception::class)
        fun t17_logout_noToken_401() {
            mockMvc.perform(MockMvcRequestBuilders.post(LOGOUT_URL))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-001"))
        }

        @Test
        @DisplayName("t18: 로그아웃 후 Cookie 없이 me 요청 → 401 (브라우저 시뮬레이션)")
        @Throws(Exception::class)
        fun t18_afterLogout_noToken_401() {
            val token = authTokenHelper.createToken(savedMember)

            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGOUT_URL)
                    .cookie(Cookie("accessToken", token))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())

            mockMvc.perform(MockMvcRequestBuilders.get(ME_URL))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-001"))
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler - 에러 응답 형식")
    internal inner class GlobalExceptionHandlerTest {
        @Test
        @DisplayName("t19: BusinessException → RFC 9457 ProblemDetail 형식")
        @Throws(Exception::class)
        fun t19_businessException_rfc9457Format() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(TEST_EMAIL, "wrongPassword!"))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.type").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-004"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.instance").exists())
        }

        @Test
        @DisplayName("t20: @Valid 실패 → validationErrors 배열 포함")
        @Throws(Exception::class)
        fun t20_validationFailed_validationErrorsArray() {
            mockMvc.perform(
                MockMvcRequestBuilders.post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("bad-email", ""))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-009"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors[0].field").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors[0].message").exists())
        }

        @Test
        @DisplayName("t21: SecurityConfig EntryPoint → RFC 9457 형식 응답")
        @Throws(Exception::class)
        fun t21_entryPoint_rfc9457Format() {
            mockMvc.perform(MockMvcRequestBuilders.get(ME_URL))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.content().contentType("application/problem+json;charset=UTF-8"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
        }

        @Test
        @DisplayName("t22: 필터에서 만료 처리 → RFC 9457 형식 응답")
        @Throws(Exception::class)
        fun t22_filterExpiredToken_rfc9457Format() {
            // ✅ AuthTokenHelper 로 만료 토큰 생성 (기존 하드코딩된 Ut.toString 제거)
            val expiredToken = authTokenHelper.createToken(savedMember, -1L)

            mockMvc.perform(
                MockMvcRequestBuilders.get(ME_URL)
                    .cookie(Cookie("accessToken", expiredToken))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.content().contentType("application/problem+json;charset=UTF-8"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-003"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
                .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
        }
    }

    @Throws(Exception::class)
    private fun loginBody(email: String, password: String): String {
        return jsonMapper.writeValueAsString(LoginRequest(email, password))
    }

    companion object {
        // ════════════════════════════════════════════════════════
        // 테스트 상수 (중앙 관리)
        // ════════════════════════════════════════════════════════
        private const val TEST_EMAIL = "auth-ctrl@example.com"
        private const val TEST_PASSWORD = "password123!"
        private const val TEST_NICKNAME = "컨트롤러테스터"
        private const val LOGIN_URL = "/api/auth/login"
        private const val ME_URL = "/api/auth/me"
        private const val LOGOUT_URL = "/api/auth/logout"
    }
}