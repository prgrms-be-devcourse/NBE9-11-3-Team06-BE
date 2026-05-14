package com.back.nbe9112team06.domain.auth.controller;

import com.back.nbe9112team06.domain.auth.dto.LoginRequest;
import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.repository.MemberRepository;
import com.back.nbe9112team06.testutil.AuthTokenHelper;
import com.back.nbe9112team06.testutil.MemberTestFactory;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired
    AuthTokenHelper authTokenHelper;
    @Autowired
    MemberTestFactory memberTestFactory;

    // ════════════════════════════════════════════════════════
    // 테스트 상수 (중앙 관리)
    // ════════════════════════════════════════════════════════
    private static final String TEST_EMAIL    = "auth-ctrl@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "컨트롤러테스터";
    private static final String LOGIN_URL     = "/api/auth/login";
    private static final String ME_URL        = "/api/auth/me";
    private static final String LOGOUT_URL    = "/api/auth/logout";

    private Member savedMember;

    @BeforeEach
    void setUp() {
        savedMember = memberTestFactory.createAndSaveMember(
                memberRepository, TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME
        );
    }

    @Nested
    @DisplayName("POST /api/auth/login - 로그인")
    class Login {

        @Test
        @DisplayName("t1: 정상 로그인 → 200, accessToken Cookie 발급")
        void t1_login_success_issuesCookie() throws Exception {
            MvcResult result = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie cookie = result.getResponse().getCookie("accessToken");
            assertThat(cookie).isNotNull();
            assertThat(cookie.getValue()).isNotBlank();
            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        @DisplayName("t2: 정상 로그인 → Response body에 accessToken 미포함")
        void t2_login_success_tokenNotInBody() throws Exception {
            MvcResult result = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("accessToken");
            assertThat(body).doesNotContain("eyJ");
        }

        @Test
        @DisplayName("t3: 정상 로그인 → Response body에 memberId, nickname 포함")
        void t3_login_success_bodyContainsMemberInfo() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME))
                    .andExpect(jsonPath("$.data.email").doesNotExist())
                    .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("t4: 정상 로그인 → resultCode 확인")
        void t4_login_success_resultCode() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("201-1"))
                    .andExpect(jsonPath("$.msg").value("로그인 성공"));
        }

        @Test
        @DisplayName("t5: 비밀번호 오류 → 401, errorCode=AUTH-004, RFC 9457 형식")
        void t5_wrongPassword_401_auth004() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, "wrongPassword!")))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType("application/problem+json"))
                    .andExpect(jsonPath("$.errorCode").value("AUTH-004"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail")
                            .value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("t6: 존재하지 않는 이메일 → 401, errorCode=AUTH-004")
        void t6_notExistEmail_401_auth004() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("ghost@example.com", TEST_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-004"));
        }

        @Test
        @DisplayName("t7: 이메일 형식 오류 → 400, errorCode=COMMON-009")
        void t7_invalidEmailFormat_400() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("not-an-email", TEST_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("t8: 비밀번호 누락 → 400, errorCode=COMMON-009")
        void t8_missingPassword_400() throws Exception {
            String body = jsonMapper.writeValueAsString(
                    Map.of("email", TEST_EMAIL)
            );

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"));
        }

        @Test
        @DisplayName("t9: Content-Type 없음 → 415 Unsupported Media Type")
        void t9_noContentType_415() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me - 내 정보 조회")
    class GetMyInfo {

        @Test
        @DisplayName("t10: 유효한 토큰으로 조회 → 200, id+nickname 반환")
        void t10_validToken_200_memberInfo() throws Exception {
            // ✅ AuthTokenHelper 활용: 토큰 생성 로직 일관화
            String token = authTokenHelper.createToken(savedMember);

            mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("accessToken", token)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME));
        }

        @Test
        @DisplayName("t11: 토큰 없음 → 401, errorCode=AUTH-001 (TOKEN_MISSING)")
        void t11_noToken_401_auth001() throws Exception {
            mockMvc.perform(get(ME_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
                    .andExpect(jsonPath("$.errorCode").value("AUTH-001"));
        }

        @Test
        @DisplayName("t12: 위조된 토큰 → 401, errorCode=AUTH-002 (TOKEN_INVALID)")
        void t12_tamperedToken_401_auth002() throws Exception {
            // ✅ AuthTokenHelper.createForgedToken() 활용: 위조 토큰 생성 명확화
            String forgedToken = authTokenHelper.createForgedToken(
                    savedMember, "fake-secret-key-for-testing-purpose!!"
            );

            mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("accessToken", forgedToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
        }

        @Test
        @DisplayName("t13: 만료된 토큰 → 401, errorCode=AUTH-003 (TOKEN_EXPIRED)")
        void t13_expiredToken_401_auth003() throws Exception {
            // ✅ AuthTokenHelper 활용: expireSeconds=-1L 로 즉시 만료 토큰 생성
            String expiredToken = authTokenHelper.createToken(savedMember, -1L);

            mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("accessToken", expiredToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-003"));
        }

        @Test
        @DisplayName("t14: 로그인 후 발급된 Cookie 로 내 정보 조회 → 연동 흐름 확인")
        void t14_loginThenMe_linkedFlow() throws Exception {
            MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, TEST_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie cookie = loginResult.getResponse().getCookie("accessToken");
            assertThat(cookie).isNotNull();

            mockMvc.perform(get(ME_URL)
                            .cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME));
        }

        @Test
        @DisplayName("t15: 응답 body 에 민감정보 미포함 확인")
        void t15_me_noSensitiveInfo() throws Exception {
            String token = authTokenHelper.createToken(savedMember);

            MvcResult result = mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("accessToken", token)))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("passwordHash");
            assertThat(body).doesNotContain(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout - 로그아웃")
    class Logout {

        @Test
        @DisplayName("t16: 인증 후 로그아웃 → 200, Cookie maxAge=0")
        void t16_logout_success_cookieExpired() throws Exception {
            String token = authTokenHelper.createToken(savedMember);

            MvcResult result = mockMvc.perform(post(LOGOUT_URL)
                            .cookie(new Cookie("accessToken", token)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.msg").value("로그아웃 성공"))
                    .andReturn();

            Cookie clearedCookie = result.getResponse().getCookie("accessToken");
            assertThat(clearedCookie).isNotNull();
            assertThat(clearedCookie.getMaxAge()).isEqualTo(0);
        }

        @Test
        @DisplayName("t17: 토큰 없이 로그아웃 → 401 (인증 필요 엔드포인트)")
        void t17_logout_noToken_401() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-001"));
        }

        @Test
        @DisplayName("t18: 로그아웃 후 Cookie 없이 me 요청 → 401 (브라우저 시뮬레이션)")
        void t18_afterLogout_noToken_401() throws Exception {
            String token = authTokenHelper.createToken(savedMember);

            mockMvc.perform(post(LOGOUT_URL)
                            .cookie(new Cookie("accessToken", token)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-001"));
        }
    }

    @Nested
    @DisplayName("GlobalExceptionHandler - 에러 응답 형식")
    class GlobalExceptionHandlerTest {

        @Test
        @DisplayName("t19: BusinessException → RFC 9457 ProblemDetail 형식")
        void t19_businessException_rfc9457Format() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(TEST_EMAIL, "wrongPassword!")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.errorCode").value("AUTH-004"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.instance").exists());
        }

        @Test
        @DisplayName("t20: @Valid 실패 → validationErrors 배열 포함")
        void t20_validationFailed_validationErrorsArray() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("bad-email", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("COMMON-009"))
                    .andExpect(jsonPath("$.validationErrors").isArray())
                    .andExpect(jsonPath("$.validationErrors[0].field").exists())
                    .andExpect(jsonPath("$.validationErrors[0].message").exists());
        }

        @Test
        @DisplayName("t21: SecurityConfig EntryPoint → RFC 9457 형식 응답")
        void t21_entryPoint_rfc9457Format() throws Exception {
            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
                    .andExpect(jsonPath("$.errorCode").value("AUTH-001"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("t22: 필터에서 만료 처리 → RFC 9457 형식 응답")
        void t22_filterExpiredToken_rfc9457Format() throws Exception {
            // ✅ AuthTokenHelper 로 만료 토큰 생성 (기존 하드코딩된 Ut.toString 제거)
            String expiredToken = authTokenHelper.createToken(savedMember, -1L);

            mockMvc.perform(get(ME_URL)
                            .cookie(new Cookie("accessToken", expiredToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
                    .andExpect(jsonPath("$.errorCode").value("AUTH-003"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    private String loginBody(String email, String password) throws Exception {
        return jsonMapper.writeValueAsString(new LoginRequest(email, password));
    }
}