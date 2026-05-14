package com.back.nbe9112team06.domain.participant.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ParticipantControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenHelper authTokenHelper;

    @Test
    @DisplayName("모임방 참가 - 성공 시 201과 participantId를 반환한다")
    void joinMeeting_success() throws Exception {
        String roomUrl = createMeetingAndGetRoomUrl("creator1@example.com", "creator1");

        mvc.perform(
                        post("/api/meetings/{randomUrl}/participants", roomUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "guestName": "홍길동",
                                          "guestPassword": "1234"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("모임방 참가 성공"))
                .andExpect(jsonPath("$.data.participantId").isNumber())
                .andExpect(jsonPath("$.data.guestName").value("홍길동"));
    }

    @Test
    @DisplayName("모임방 참가 - 존재하지 않는 randomUrl이면 404를 반환한다")
    void joinMeeting_fail_whenMeetingNotFound() throws Exception {
        mvc.perform(
                        post("/api/meetings/{randomUrl}/participants", "notExistsUrl")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "guestName": "홍길동",
                                          "guestPassword": "1234"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("모임방 참가 - 같은 이름으로 중복 참가해도 모두 성공한다")
    void joinMeeting_success_whenDuplicateGuestNameAllowed() throws Exception {
        String roomUrl = createMeetingAndGetRoomUrl("creator2@example.com", "creator2");

        mvc.perform(
                        post("/api/meetings/{randomUrl}/participants", roomUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "guestName": "중복이름",
                                          "guestPassword": "1111"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated());

        mvc.perform(
                        post("/api/meetings/{randomUrl}/participants", roomUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "guestName": "중복이름",
                                          "guestPassword": "2222"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.guestName").value("중복이름"));
    }

    @Test
    @DisplayName("모임방 참가 - 이름 또는 비밀번호가 공백이면 400을 반환한다")
    void joinMeeting_fail_whenInvalidRequest() throws Exception {
        String roomUrl = createMeetingAndGetRoomUrl("creator3@example.com", "creator3");

        mvc.perform(
                        post("/api/meetings/{randomUrl}/participants", roomUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "guestName": "",
                                          "guestPassword": "1234"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("모임방 참가 - 유효하지 않은 토큰 쿠키가 있어도 permitAll로 참가 가능하다")
    void joinMeeting_success_evenWithInvalidTokenCookie() throws Exception {
        String roomUrl = createMeetingAndGetRoomUrl("creator4@example.com", "creator4");

        mvc.perform(
                        post("/api/meetings/{randomUrl}/participants", roomUrl)
                                .cookie(new Cookie("accessToken", "invalid.jwt.token"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "guestName": "비회원참가자",
                                          "guestPassword": "9999"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.guestName").value("비회원참가자"));
    }

    private String createMeetingAndGetRoomUrl(String email, String nickname) throws Exception {
        Member member = memberRepository.save(new Member(
                email,
                "hashedPassword",
                nickname,
                TimezoneType.ASIA_SEOUL
        ));
        String token = authTokenHelper.createToken(member);

        String createResponse = mvc.perform(
                        post("/api/meetings")
                                .cookie(new Cookie("accessToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "참가 테스트 모임",
                                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                                          "duration": 60,
                                          "category": "PROJECT"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractValue(createResponse, "\"roomUrl\":\"", "\"");
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
