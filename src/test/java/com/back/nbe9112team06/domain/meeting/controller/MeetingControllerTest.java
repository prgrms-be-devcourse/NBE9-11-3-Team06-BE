package com.back.nbe9112team06.domain.meeting.controller;

import com.back.nbe9112team06.domain.member.entity.Member;
import com.back.nbe9112team06.domain.member.entity.TimezoneType;
import com.back.nbe9112team06.domain.member.repository.MemberRepository;
import com.back.nbe9112team06.testutil.AuthTokenHelper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private Member saveMember(String email) {
        return memberRepository.save(new Member(email, "hashedPassword", "tester", TimezoneType.ASIA_SEOUL));
    }

    private String createMeetingAndGetField(String token, String fieldPath) throws Exception {
        String response = mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "테스트 모임",
                                          "dates": ["2026-04-20", "2026-04-21"],
                                          "duration": 60,
                                          "category": "STUDY"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return extractValue(response, "\"" + fieldPath + "\":", ",", "}");
    }

    private String getRoomUrl(String token) throws Exception {
        return createMeetingAndGetField(token, "roomUrl").replace("\"", "").trim();
    }

    private int getMeetingId(String token) throws Exception {
        return Integer.parseInt(createMeetingAndGetField(token, "meetingId").trim());
    }

    private void addParticipant(String roomUrl) throws Exception {
        mvc.perform(
                post("/api/meetings/{roomUrl}/participants", roomUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "guestName": "게스트", "guestPassword": "1234" }
                                """)
        ).andExpect(status().isCreated());
    }

    private void confirmMeeting(int meetingId, String token) throws Exception {
        mvc.perform(
                post("/api/meetings/{meetingId}/confirm", meetingId)
                        .cookie(new Cookie("accessToken", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2026-04-20", "time": "14:00" }
                                """)
        ).andExpect(status().isOk());
    }

    private String extractValue(String raw, String startToken, String... endTokens) {
        int start = raw.indexOf(startToken);
        if (start == -1) throw new IllegalStateException("토큰을 찾을 수 없습니다: " + startToken);
        start += startToken.length();
        int end = raw.length();
        for (String endToken : endTokens) {
            int idx = raw.indexOf(endToken, start);
            if (idx != -1 && idx < end) end = idx;
        }
        return raw.substring(start, end);
    }

    // ── 모임 생성 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/meetings")
    class CreateMeeting {

        @Test
        @DisplayName("성공 - 201과 meetingId, roomUrl 반환")
        void createMeeting_success() throws Exception {
            Member member = saveMember("creator@example.com");
            String token = authTokenHelper.createToken(member);

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
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultCode").value("201-1"))
                    .andExpect(jsonPath("$.data.meetingId").isNumber())
                    .andExpect(jsonPath("$.data.roomUrl").isString());
        }

        @Test
        @DisplayName("성공 - 로그인 API에서 발급된 쿠키로도 생성 가능")
        void createMeeting_success_withLoginCookie() throws Exception {
            String email = "creator-login@example.com";
            String rawPassword = "password123!";
            memberRepository.save(new Member(email, passwordEncoder.encode(rawPassword), "creator-login", TimezoneType.ASIA_SEOUL));

            Cookie loginCookie = mvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            { "email": "%s", "password": "%s" }
                                            """.formatted(email, rawPassword))
                    )
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getCookie("accessToken");

            mvc.perform(
                            post("/api/meetings")
                                    .cookie(loginCookie)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "title": "로그인 흐름 테스트",
                                              "dates": ["2026-04-20"],
                                              "duration": 30,
                                              "category": "PROJECT"
                                            }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.roomUrl").isString());
        }

        @Test
        @DisplayName("실패 - 비로그인 시 401")
        void createMeeting_fail_unauthorized() throws Exception {
            mvc.perform(
                            post("/api/meetings")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "title": "팀 회의",
                                              "dates": ["2026-04-20"],
                                              "duration": 60,
                                              "category": "PROJECT"
                                            }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 위조 토큰이면 401")
        void createMeeting_fail_invalidToken() throws Exception {
            mvc.perform(
                            post("/api/meetings")
                                    .cookie(new Cookie("accessToken", "invalid.jwt.token"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "title": "팀 회의",
                                              "dates": ["2026-04-20"],
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
        @DisplayName("실패 - 날짜 미입력 시 400")
        void createMeeting_fail_datesEmpty() throws Exception {
            Member member = saveMember("creator2@example.com");
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
        @DisplayName("실패 - 존재하지 않는 회원이면 404")
        void createMeeting_fail_memberNotFound() throws Exception {
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
                                              "dates": ["2026-04-20"],
                                              "duration": 60,
                                              "category": "PROJECT"
                                            }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ── 랜덤 URL 조회 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/meetings/{randomUrl}")
    class GetMeetingByRandomUrl {

        @Test
        @DisplayName("성공 - 200과 모임 상세 정보 반환")
        void getMeetingByRandomUrl_success() throws Exception {
            Member member = saveMember("creator3@example.com");
            String token = authTokenHelper.createToken(member);
            String roomUrl = getRoomUrl(token);

            mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.data.roomUrl").value(roomUrl))
                    .andExpect(jsonPath("$.data.title").value("테스트 모임"));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 URL이면 404")
        void getMeetingByRandomUrl_fail_notFound() throws Exception {
            mvc.perform(get("/api/meetings/{randomUrl}", "notExistsUrl"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ── 내 모임 목록 조회 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/meetings")
    class GetMyMeetings {

        @Test
        @DisplayName("성공 - 내가 만든 모임 목록 반환")
        void getMyMeetings_success() throws Exception {
            Member member = saveMember("list@example.com");
            String token = authTokenHelper.createToken(member);
            getRoomUrl(token); // 모임 1개 생성

            mvc.perform(
                            get("/api/meetings")
                                    .cookie(new Cookie("accessToken", token))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].title").value("테스트 모임"));
        }

        @Test
        @DisplayName("성공 - 모임이 없으면 빈 목록 반환")
        void getMyMeetings_empty() throws Exception {
            Member member = saveMember("empty@example.com");
            String token = authTokenHelper.createToken(member);

            mvc.perform(
                            get("/api/meetings")
                                    .cookie(new Cookie("accessToken", token))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("실패 - 비로그인 시 401")
        void getMyMeetings_fail_unauthorized() throws Exception {
            mvc.perform(get("/api/meetings"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── 모임 삭제 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/meetings/{meetingId}")
    class DeleteMeeting {

        @Test
        @DisplayName("성공 - 방장이 삭제 시 200")
        void deleteMeeting_success() throws Exception {
            Member member = saveMember("delete-host@example.com");
            String token = authTokenHelper.createToken(member);
            int meetingId = getMeetingId(token);

            mvc.perform(
                            delete("/api/meetings/{meetingId}", meetingId)
                                    .cookie(new Cookie("accessToken", token))
                    )
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 - 방장이 아닌 회원이 삭제 시도 시 403")
        void deleteMeeting_fail_notHost() throws Exception {
            Member host = saveMember("delete-host2@example.com");
            Member other = saveMember("delete-other@example.com");
            String hostToken = authTokenHelper.createToken(host);
            String otherToken = authTokenHelper.createToken(other);
            int meetingId = getMeetingId(hostToken);

            mvc.perform(
                            delete("/api/meetings/{meetingId}", meetingId)
                                    .cookie(new Cookie("accessToken", otherToken))
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("MEETING-002"));
        }

        @Test
        @DisplayName("실패 - 비로그인 시 401")
        void deleteMeeting_fail_unauthorized() throws Exception {
            mvc.perform(delete("/api/meetings/{meetingId}", 1))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── 일정 확정 ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/meetings/{meetingId}/confirm")
    class Confirm {

        @Test
        @DisplayName("성공 - 참여자 있는 모임 확정 시 200")
        void confirm_success() throws Exception {
            Member member = saveMember("confirm-host@example.com");
            String token = authTokenHelper.createToken(member);
            String roomUrl = getRoomUrl(token);
            int meetingId = Integer.parseInt(
                    extractValue(
                            mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                                    .andReturn().getResponse().getContentAsString(),
                            "\"meetingId\":", ",", "}"
                    ).trim()
            );
            addParticipant(roomUrl);

            mvc.perform(
                            post("/api/meetings/{meetingId}/confirm", meetingId)
                                    .cookie(new Cookie("accessToken", token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            { "date": "2026-04-20", "time": "14:00" }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("실패 - 참여자 없는 모임 확정 시 400")
        void confirm_fail_noParticipants() throws Exception {
            Member member = saveMember("confirm-empty@example.com");
            String token = authTokenHelper.createToken(member);
            int meetingId = getMeetingId(token);

            mvc.perform(
                            post("/api/meetings/{meetingId}/confirm", meetingId)
                                    .cookie(new Cookie("accessToken", token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            { "date": "2026-04-20", "time": "14:00" }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MEETING-005"));
        }

        @Test
        @DisplayName("실패 - 방장이 아닌 회원이 확정 시도 시 403")
        void confirm_fail_notHost() throws Exception {
            Member host = saveMember("confirm-host2@example.com");
            Member other = saveMember("confirm-other@example.com");
            String hostToken = authTokenHelper.createToken(host);
            String otherToken = authTokenHelper.createToken(other);
            int meetingId = getMeetingId(hostToken);

            mvc.perform(
                            post("/api/meetings/{meetingId}/confirm", meetingId)
                                    .cookie(new Cookie("accessToken", otherToken))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            { "date": "2026-04-20", "time": "14:00" }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("MEETING-002"));
        }

        @Test
        @DisplayName("실패 - 비로그인 시 401")
        void confirm_fail_unauthorized() throws Exception {
            mvc.perform(
                            post("/api/meetings/{meetingId}/confirm", 1)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            { "date": "2026-04-20", "time": "14:00" }
                                            """)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── 일정 확정 취소 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/meetings/{meetingId}/confirm")
    class CancelConfirm {

        @Test
        @DisplayName("성공 - 확정된 모임 취소 시 200")
        void cancelConfirm_success() throws Exception {
            Member member = saveMember("cancel-host@example.com");
            String token = authTokenHelper.createToken(member);
            String roomUrl = getRoomUrl(token);
            int meetingId = Integer.parseInt(
                    extractValue(
                            mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                                    .andReturn().getResponse().getContentAsString(),
                            "\"meetingId\":", ",", "}"
                    ).trim()
            );
            addParticipant(roomUrl);
            confirmMeeting(meetingId, token);

            mvc.perform(
                            delete("/api/meetings/{meetingId}/confirm", meetingId)
                                    .cookie(new Cookie("accessToken", token))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"));
        }

        @Test
        @DisplayName("실패 - 방장이 아닌 회원이 취소 시도 시 403")
        void cancelConfirm_fail_notHost() throws Exception {
            Member host = saveMember("cancel-host2@example.com");
            Member other = saveMember("cancel-other@example.com");
            String hostToken = authTokenHelper.createToken(host);
            String otherToken = authTokenHelper.createToken(other);
            String roomUrl = getRoomUrl(hostToken);
            int meetingId = Integer.parseInt(
                    extractValue(
                            mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                                    .andReturn().getResponse().getContentAsString(),
                            "\"meetingId\":", ",", "}"
                    ).trim()
            );
            addParticipant(roomUrl);
            confirmMeeting(meetingId, hostToken);

            mvc.perform(
                            delete("/api/meetings/{meetingId}/confirm", meetingId)
                                    .cookie(new Cookie("accessToken", otherToken))
                    )
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("MEETING-002"));
        }
    }

    // ── 확정 일정 조회 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/meetings/{meetingId}/confirm")
    class GetConfirmedSchedule {

        @Test
        @DisplayName("성공 - 확정된 모임의 일정 정보 반환")
        void getConfirmedSchedule_success() throws Exception {
            Member member = saveMember("schedule-host@example.com");
            String token = authTokenHelper.createToken(member);
            String roomUrl = getRoomUrl(token);
            int meetingId = Integer.parseInt(
                    extractValue(
                            mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                                    .andReturn().getResponse().getContentAsString(),
                            "\"meetingId\":", ",", "}"
                    ).trim()
            );
            addParticipant(roomUrl);
            confirmMeeting(meetingId, token);

            mvc.perform(get("/api/meetings/{meetingId}/confirm", meetingId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.date").value("2026-04-20"));
        }

        @Test
        @DisplayName("실패 - 미확정 모임 조회 시 400")
        void getConfirmedSchedule_fail_notConfirmed() throws Exception {
            Member member = saveMember("schedule-pending@example.com");
            String token = authTokenHelper.createToken(member);
            int meetingId = getMeetingId(token);

            mvc.perform(get("/api/meetings/{meetingId}/confirm", meetingId))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("MEETING-004"));
        }
    }
}
