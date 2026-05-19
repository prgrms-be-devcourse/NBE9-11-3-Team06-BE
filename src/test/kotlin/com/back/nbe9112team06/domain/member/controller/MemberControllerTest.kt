package com.back.nbe9112team06.domain.member.controller

import com.back.nbe9112team06.domain.member.dto.SignupRequest
import com.back.nbe9112team06.domain.member.dto.request.CheckEmailRequest
import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
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
@DisplayName("MemberController 통합 테스트")
class MemberControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var memberTestFactory: MemberTestFactory

    @Autowired
    private lateinit var authTokenHelper: AuthTokenHelper

    private lateinit var savedMember: Member
    private lateinit var validToken: String

    @BeforeEach
    fun setUp() {
        savedMember = memberTestFactory.createAndSaveMember(
            memberRepository, TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME
        )

        validToken = authTokenHelper.createToken(savedMember)
    }

    @Nested
    @DisplayName("POST /api/members - 회원가입")
    inner class Signup {
        @Test
        @DisplayName("t1: 정상 회원가입 → 200, 회원가입 성공 응답")
        @Throws(Exception::class)
        fun t1_signup_success() {
            // given: 중복되지 않은 이메일
            val uniqueEmail = "new-" + System.currentTimeMillis() + "@example.com"
            val request = SignupRequest(
                uniqueEmail, TEST_PASSWORD, "새회원", TimezoneType.ASIA_SEOUL
            )

            // when & then
            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk()) // ApiResponse 기준 200
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원가입에 성공하셨습니다"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(uniqueEmail))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value("새회원")) // 민감정보 노출 방지
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.password").doesNotExist())
        }

        @Test
        @DisplayName("t2: 중복 이메일 → 409, DUPLICATE_EMAIL")
        @Throws(Exception::class)
        fun t2_signup_duplicateEmail() {
            // given: 이미 존재하는 이메일
            val request = SignupRequest(
                TEST_EMAIL, TEST_PASSWORD, "다른닉네임", TimezoneType.ASIA_SEOUL
            )

            // when & then
            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isConflict()) // 또는 프로젝트 규칙에 따른 상태코드
                .andExpect(MockMvcResultMatchers.content().contentType("application/problem+json"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("MEMBER-002")) // 실제 에러코드 확인
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("이미 등록된 이메일입니다."))
        }

        @Test
        @DisplayName("t3: 이메일 형식 오류 → 400, COMMON-009")
        @Throws(Exception::class)
        fun t3_signup_invalidEmailFormat() {
            val request = SignupRequest(
                "not-an-email", TEST_PASSWORD, TEST_NICKNAME, TimezoneType.ASIA_SEOUL
            )

            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-009"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors[0].field").value("email"))
        }

        @Test
        @DisplayName("t4: 비밀번호 길이 위반 → 400, COMMON-009")
        @Throws(Exception::class)
        fun t4_signup_invalidPasswordLength() {
            val request = SignupRequest(
                "valid@example.com", "short", TEST_NICKNAME, TimezoneType.ASIA_SEOUL
            )

            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-009"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isArray())
        }

        //
        @Test
        @DisplayName("t5: 필수 필드 누락 → 400, COMMON-002")
        @Throws(Exception::class)
        fun t5_signup_missingRequiredField() {
            // email 만 포함한 불완전한 요청
            val body = jsonMapper.writeValueAsString(mapOf("email" to "test@example.com"))

            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-002"))
        }
    }

    @Nested
    @DisplayName("POST /api/members/check-email - 이메일 중복 체크")
    inner class CheckEmail {
        @Test
        @DisplayName("t6: 사용 가능한 이메일 → 200, available=true")
        @Throws(Exception::class)
        fun t6_checkEmail_available() {
            val request = CheckEmailRequest("new-user@example.com")

            mockMvc.perform(
                MockMvcRequestBuilders.post(CHECK_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.available").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("사용 가능한 이메일입니다."))
        }

        @Test
        @DisplayName("t7: 이미 등록된 이메일 → 200, available=false")
        @Throws(Exception::class)
        fun t7_checkEmail_alreadyRegistered() {
            val request = CheckEmailRequest(TEST_EMAIL)

            mockMvc.perform(
                MockMvcRequestBuilders.post(CHECK_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.available").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 등록된 이메일입니다."))
        }

        @Test
        @DisplayName("t8: 이메일 형식 오류 → 400, COMMON-009")
        @Throws(Exception::class)
        fun t8_checkEmail_invalidFormat() {
            val request = CheckEmailRequest("invalid-email")

            mockMvc.perform(
                MockMvcRequestBuilders.post(CHECK_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-009"))
        }

        @Test
        @DisplayName("t9: 이메일 필드 누락 → 400, COMMON-002")
        @Throws(Exception::class)
        fun t9_checkEmail_missingEmail() {
            val body = jsonMapper.writeValueAsString(mapOf<Any, Any>()) // 빈 객체

            mockMvc.perform(
                MockMvcRequestBuilders.post(CHECK_EMAIL_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-002"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/members - 회원 탈퇴")
    internal inner class DeleteMember {
        @Test
        @DisplayName("t10: 인증 후 정상 탈퇴 → 200, Cookie 만료")
        @Throws(Exception::class)
        fun t10_delete_success_withAuth() {
            // given: 유효한 토큰으로 인증된 상태

            // when & then

            val result = mockMvc.perform(
                MockMvcRequestBuilders.delete(DELETE_URL)
                    .cookie(Cookie("accessToken", validToken))
            )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 탈퇴가 완료되었습니다."))
                .andReturn()

            // then: 로그아웃과 동일하게 accessToken Cookie 만료 처리 확인
            val clearedCookie = result.response.getCookie("accessToken")
            Assertions.assertThat<Cookie?>(clearedCookie).isNotNull()
            Assertions.assertThat(clearedCookie?.maxAge).isEqualTo(0)

            // 실제 삭제 확인 (DB 조회)
            Assertions.assertThat(memberRepository.findById(savedMember.id)).isEmpty()
        }

        @Test
        @DisplayName("t11: 만료된 토큰 → 401, AUTH-003")
        @Throws(Exception::class)
        fun t11_delete_expiredToken() {
            val expiredToken = authTokenHelper.createToken(savedMember, -1L)

            mockMvc.perform(
                MockMvcRequestBuilders.delete(DELETE_URL)
                    .cookie(Cookie("accessToken", expiredToken))
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("AUTH-003"))
        }

        @Test
        @DisplayName("t12: 존재하지 않는 회원 탈퇴 시도 → 404, NOT_FOUND")
        @Throws(Exception::class)
        fun t12_delete_nonExistentMember() {
            // given: 존재하지 않는 id 를 가진 토큰 생성 (수동으로 payload 조작)
            val token = authTokenHelper.createTokenWithPayload(
                mapOf("id" to 99999, "nickname" to "존재하지않는회원")
            )

            mockMvc.perform(
                MockMvcRequestBuilders.delete(DELETE_URL)
                    .cookie(Cookie("accessToken", token))
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-003")) // 실제 에러코드 확인
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler 연동")
    internal inner class GlobalExceptionHandling {
        @Test
        @DisplayName("t15: BusinessException → RFC 9457 ProblemDetail 형식")
        @Throws(Exception::class)
        fun t15_businessException_rfc9457Format() {
            val request = SignupRequest(
                TEST_EMAIL, TEST_PASSWORD, "dup", TimezoneType.ASIA_SEOUL
            ) // 중복 이메일

            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.content().contentType("application/problem+json")) // RFC 9457 필수 필드
                .andExpect(MockMvcResultMatchers.jsonPath("$.type").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").exists()) // 커스텀 확장 필드
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.instance").exists())
        }

        @Test
        @DisplayName("t16: @Valid 실패 → validationErrors 배열 포함")
        @Throws(Exception::class)
        fun t16_validationFailed_validationErrorsArray() {
            val request = SignupRequest(
                "bad-email", "pw", "n", TimezoneType.ASIA_SEOUL
            ) // 여러 검증 실패

            mockMvc.perform(
                MockMvcRequestBuilders.post(SIGNUP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request))
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors[0].field").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors[0].message").exists())
        }
    }


    @Throws(Exception::class)
    private fun jsonBody(obj: Any): String {
        return jsonMapper.writeValueAsString(obj)
    }

    companion object {
        // ════════════════════════════════════════════════════════
        // 테스트 상수 (중앙 관리)
        // ════════════════════════════════════════════════════════
        private const val SIGNUP_URL = "/api/members"
        private const val CHECK_EMAIL_URL = "/api/members/check-email"
        private const val DELETE_URL = "/api/members"

        private const val TEST_EMAIL = "member-ctrl@example.com"
        private const val TEST_PASSWORD = "password123!"
        private const val TEST_NICKNAME = "회원테스터"
    }
}