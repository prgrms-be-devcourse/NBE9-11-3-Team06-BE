package com.back.nbe9112team06.domain.member.controller;

import com.back.nbe9112team06.domain.member.dto.SignupRequest;
import com.back.nbe9112team06.domain.member.dto.request.CheckEmailRequest;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.domain.member.repository.MemberRepository;
import com.back.nbe9112team06.testutil.AuthTokenHelper;
import com.back.nbe9112team06.testutil.MemberTestFactory;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("MemberController 통합 테스트")
class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberTestFactory memberTestFactory;
    @Autowired private AuthTokenHelper authTokenHelper;

    // ════════════════════════════════════════════════════════
    // 테스트 상수 (중앙 관리)
    // ════════════════════════════════════════════════════════
    private static final String SIGNUP_URL = "/api/members";
    private static final String CHECK_EMAIL_URL = "/api/members/check-email";
    private static final String DELETE_URL = "/api/members";

    private static final String TEST_EMAIL = "member-ctrl@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "회원테스터";

    private Member savedMember;
    private String validToken;

    @BeforeEach
    void setUp() {
        savedMember = memberTestFactory.createAndSaveMember(
                memberRepository, TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME);

        validToken = authTokenHelper.createToken(savedMember);
    }

    @Nested
    @DisplayName("POST /api/members - 회원가입")
    class Signup {

        @Test
        @DisplayName("t1: 정상 회원가입 → 200, 회원가입 성공 응답")
        void t1_signup_success() throws Exception {
            // given: 중복되지 않은 이메일
            String uniqueEmail = "new-" + System.currentTimeMillis() + "@example.com";
            SignupRequest request = new SignupRequest(
                    uniqueEmail, TEST_PASSWORD, "새회원", TimezoneType.ASIA_SEOUL);

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())  // ApiResponse 기준 200
                    .andExpect(jsonPath("$.resultCode").value("201-1"))
                    .andExpect(jsonPath("$.msg").value("회원가입에 성공하셨습니다"))
                    .andExpect(jsonPath("$.data.email").value(uniqueEmail))
                    .andExpect(jsonPath("$.data.nickname").value("새회원"))
                    // 민감정보 노출 방지
                    .andExpect(jsonPath("$.data.password").doesNotExist());
        }

        @Test
        @DisplayName("t2: 중복 이메일 → 409, DUPLICATE_EMAIL")
        void t2_signup_duplicateEmail() throws Exception {
            // given: 이미 존재하는 이메일
            SignupRequest request = new SignupRequest(
                    TEST_EMAIL, TEST_PASSWORD, "다른닉네임", TimezoneType.ASIA_SEOUL);

            // when & then
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())  // 또는 프로젝트 규칙에 따른 상태코드
                    .andExpect(content().contentType("application/problem+json"))
                    .andExpect(jsonPath("$.errorCode").value("MEMBER-002"))  // 실제 에러코드 확인
                    .andExpect(jsonPath("$.detail").value("이미 등록된 이메일입니다."));
        }

        @Test
        @DisplayName("t3: 이메일 형식 오류 → 400, COMMON-009")
        void t3_signup_invalidEmailFormat() throws Exception {
            SignupRequest request = new SignupRequest(
                    "not-an-email", TEST_PASSWORD, TEST_NICKNAME, TimezoneType.ASIA_SEOUL);

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"))
                    .andExpect(jsonPath("$.validationErrors").isArray())
                    .andExpect(jsonPath("$.validationErrors[0].field").value("email"));
        }

        @Test
        @DisplayName("t4: 비밀번호 길이 위반 → 400, COMMON-009")
        void t4_signup_invalidPasswordLength() throws Exception {
            SignupRequest request = new SignupRequest(
                    "valid@example.com", "short", TEST_NICKNAME, TimezoneType.ASIA_SEOUL);

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("t5: 필수 필드 누락 → 400, COMMON-009")
        void t5_signup_missingRequiredField() throws Exception {
            // email 만 포함한 불완전한 요청
            String body = jsonMapper.writeValueAsString(Map.of("email", "test@example.com"));

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"));
        }
    }

    @Nested
    @DisplayName("POST /api/members/check-email - 이메일 중복 체크")
    class CheckEmail {

        @Test
        @DisplayName("t6: 사용 가능한 이메일 → 200, available=true")
        void t6_checkEmail_available() throws Exception {
            CheckEmailRequest request = new CheckEmailRequest("new-user@example.com");

            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.data.available").value(true))
                    .andExpect(jsonPath("$.msg").value("사용 가능한 이메일입니다."));
        }

        @Test
        @DisplayName("t7: 이미 등록된 이메일 → 200, available=false")
        void t7_checkEmail_alreadyRegistered() throws Exception {
            CheckEmailRequest request = new CheckEmailRequest(TEST_EMAIL);

            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(false))
                    .andExpect(jsonPath("$.msg").value("이미 등록된 이메일입니다."));
        }

        @Test
        @DisplayName("t8: 이메일 형식 오류 → 400, COMMON-009")
        void t8_checkEmail_invalidFormat() throws Exception {
            CheckEmailRequest request = new CheckEmailRequest("invalid-email");

            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"));
        }

        @Test
        @DisplayName("t9: 이메일 필드 누락 → 400, COMMON-009")
        void t9_checkEmail_missingEmail() throws Exception {
            String body = jsonMapper.writeValueAsString(Map.of());  // 빈 객체

            mockMvc.perform(post(CHECK_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/members - 회원 탈퇴")
    class DeleteMember {

        @Test
        @DisplayName("t10: 인증 후 정상 탈퇴 → 200, Cookie 만료")
        void t10_delete_success_withAuth() throws Exception {
            // given: 유효한 토큰으로 인증된 상태

            // when & then
            MvcResult result = mockMvc.perform(delete(DELETE_URL)
                            .cookie(new Cookie("accessToken", validToken)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.msg").value("회원 탈퇴가 완료되었습니다."))
                    .andReturn();

            // then: 로그아웃과 동일하게 accessToken Cookie 만료 처리 확인
            Cookie clearedCookie = result.getResponse().getCookie("accessToken");
            assertThat(clearedCookie).isNotNull();
            assertThat(clearedCookie.getMaxAge()).isEqualTo(0);

            // 실제 삭제 확인 (DB 조회)
            assertThat(memberRepository.findById(savedMember.getId())).isEmpty();
        }

        @Test
        @DisplayName("t11: 만료된 토큰 → 401, AUTH-003")
        void t11_delete_expiredToken() throws Exception {
            String expiredToken = authTokenHelper.createToken(savedMember, -1L);

            mockMvc.perform(delete(DELETE_URL)
                            .cookie(new Cookie("accessToken", expiredToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-003"));
        }

        @Test
        @DisplayName("t12: 존재하지 않는 회원 탈퇴 시도 → 404, NOT_FOUND")
        void t12_delete_nonExistentMember() throws Exception {
            // given: 존재하지 않는 id 를 가진 토큰 생성 (수동으로 payload 조작)
            String token = authTokenHelper.createTokenWithPayload(
                    Map.of("id", 99999, "nickname", "존재하지않는회원"));

            mockMvc.perform(delete(DELETE_URL)
                            .cookie(new Cookie("accessToken", token)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-003"));  // 실제 에러코드 확인
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler 연동")
    class GlobalExceptionHandling {

        @Test
        @DisplayName("t15: BusinessException → RFC 9457 ProblemDetail 형식")
        void t15_businessException_rfc9457Format() throws Exception {
            SignupRequest request = new SignupRequest(
                    TEST_EMAIL, TEST_PASSWORD, "dup", TimezoneType.ASIA_SEOUL);  // 중복 이메일

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType("application/problem+json"))
                    // RFC 9457 필수 필드
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.detail").exists())
                    // 커스텀 확장 필드
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.instance").exists());
        }

        @Test
        @DisplayName("t16: @Valid 실패 → validationErrors 배열 포함")
        void t16_validationFailed_validationErrorsArray() throws Exception {
            SignupRequest request = new SignupRequest(
                    "bad-email", "pw", "n", TimezoneType.ASIA_SEOUL);  // 여러 검증 실패

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").isArray())
                    .andExpect(jsonPath("$.validationErrors").isNotEmpty())
                    .andExpect(jsonPath("$.validationErrors[0].field").exists())
                    .andExpect(jsonPath("$.validationErrors[0].message").exists());
        }
    }


    private String jsonBody(Object obj) throws Exception {
        return jsonMapper.writeValueAsString(obj);
    }
}