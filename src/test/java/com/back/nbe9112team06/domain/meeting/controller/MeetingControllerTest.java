package com.back.nbe9112team06.domain.meeting.controller;

import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.domain.member.repository.MemberRepository;
import com.back.nbe9112team06.testutil.AuthTokenHelper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MeetingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenHelper authTokenHelper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("모임방 생성 - 성공 시 201과 랜덤 URL을 반환한다")
    void createMeeting_success() throws Exception {
        Member member = memberRepository.save(new Member(
                "creator@example.com",
                "hashedPassword",
                "creator",
                TimezoneType.ASIA_SEOUL
        ));
        String token = authTokenHelper.createToken(member);

        ResultActions resultActions = mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "팀 회의",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 60,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("모임방 생성 성공"))
                .andExpect(jsonPath("$.data.meetingId").isNumber())
                .andExpect(jsonPath("$.data.roomUrl").isString());
    }

    @Test
    @DisplayName("모임방 생성 - 로그인 API에서 발급된 쿠키로도 생성 가능하다")
    void createMeeting_success_withLoginCookie() throws Exception {
        String email = "creator-login@example.com";
        String rawPassword = "password123!";

        memberRepository.save(new Member(
                email,
                passwordEncoder.encode(rawPassword),
                "creator-login",
                TimezoneType.ASIA_SEOUL
        ));

        Cookie loginCookie = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, rawPassword))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("accessToken");

        mvc.perform(
                        post("/api/meetings")
                                .cookie(loginCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "로그인 흐름 테스트",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 30,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.roomUrl").isString());
    }

    @Test
    @DisplayName("모임방 생성 - 로그인하지 않으면 401을 반환한다")
    void createMeeting_fail_whenUnauthorized() throws Exception {
        mvc.perform(
                        post("/api/meetings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "팀 회의",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 60,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("모임방 생성 - 위조 토큰 쿠키면 401을 반환한다")
    void createMeeting_fail_whenInvalidToken() throws Exception {
        mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", "invalid.jwt.token"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "팀 회의",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 60,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
    }

    @Test
    @DisplayName("모임방 생성 - 날짜를 입력하지 않으면 400을 반환한다")
    void createMeeting_fail_whenDatesEmpty() throws Exception {
        Member member = memberRepository.save(new Member(
                "creator2@example.com",
                "hashedPassword",
                "creator2",
                TimezoneType.ASIA_SEOUL
        ));
        String token = authTokenHelper.createToken(member);

        mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "팀 회의",
                                          "duration": 60,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("모임방 생성 - 존재하지 않는 회원이면 404를 반환한다")
    void createMeeting_fail_whenMemberNotFound() throws Exception {
        String token = authTokenHelper.createTokenWithPayload(
                java.util.Map.of("id", 999999, "nickname", "ghost")
        );

        mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "팀 회의",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 60,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("랜덤 URL 조회 - 존재하는 모임방이면 200과 상세 정보를 반환한다")
    void getMeetingByRandomUrl_success() throws Exception {
        Member member = memberRepository.save(new Member(
                "creator3@example.com",
                "hashedPassword",
                "creator3",
                TimezoneType.ASIA_SEOUL
        ));
        String token = authTokenHelper.createToken(member);

        String createResponse = mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "입장 테스트 방",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 45,
                                          "category": "STUDY"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String roomUrl = extractValue(createResponse, "\"roomUrl\":\"", "\"");

        mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("모임방 조회 성공"))
                .andExpect(jsonPath("$.data.roomUrl").value(roomUrl))
                .andExpect(jsonPath("$.data.title").value("입장 테스트 방"))
                .andExpect(jsonPath("$.data.dates[0]").value("2026-04-20"));
    }

    @Test
    @DisplayName("랜덤 URL 조회 - 존재하지 않는 모임방이면 404를 반환한다")
    void getMeetingByRandomUrl_fail_whenNotFound() throws Exception {
        mvc.perform(get("/api/meetings/{randomUrl}", "notExistsUrl"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private String extractValue(String raw, String startToken, String endToken) {
        int start = raw.indexOf(startToken);
        if (start == -1) {
            throw new IllegalStateException("응답에서 roomUrl을 찾을 수 없습니다.");
        }
        start += startToken.length();
        int end = raw.indexOf(endToken, start);
        return raw.substring(start, end);
    }
}
