package com.back.nbe9112team06.domain.meeting.controller

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import com.back.nbe9112team06.testutil.AuthTokenHelper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MeetingControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var authTokenHelper: AuthTokenHelper
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────────────

    private fun saveMember(email: String): Member =
        memberRepository.save(Member(email, "hashedPassword", "tester", TimezoneType.ASIA_SEOUL))

    private fun String.extractIntValue(key: String): Int =
        substringAfter("\"$key\":").substringBefore(",").substringBefore("}").trim().toInt()

    private fun String.extractStringValue(key: String): String =
        substringAfter("\"$key\":\"").substringBefore("\"")

    private fun createMeetingResponse(token: String): String =
        mvc.perform(
            post("/api/meetings")
                .cookie(Cookie("accessToken", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "테스트 모임",
                      "dates": ["2026-04-20", "2026-04-21"],
                      "duration": 60,
                      "category": "STUDY"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated())
            .andReturn().response.contentAsString

    private fun getRoomUrl(token: String): String =
        createMeetingResponse(token).extractStringValue("roomUrl")

    private fun getMeetingId(token: String): Int =
        createMeetingResponse(token).extractIntValue("meetingId")

    private fun getMeetingIdByUrl(roomUrl: String): Int =
        mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
            .andReturn().response.contentAsString
            .extractIntValue("meetingId")

    private fun addParticipant(roomUrl: String) {
        mvc.perform(
            post("/api/meetings/{roomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "guestName": "게스트", "guestPassword": "1234" }""")
        ).andExpect(status().isCreated())
    }

    private fun confirmMeeting(meetingId: Int, token: String) {
        mvc.perform(
            post("/api/meetings/{meetingId}/confirm", meetingId)
                .cookie(Cookie("accessToken", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "date": "2026-04-20", "time": "14:00" }""")
        ).andExpect(status().isOk())
    }

    // ── 모임 생성 ─────────────────────────────────────────────────────────────

    @Nested
    inner class `POST api meetings` {

        @Test
        fun `성공 - 201과 meetingId, roomUrl 반환`() {
            val token = authTokenHelper.createToken(saveMember("creator@example.com"))

            mvc.perform(
                post("/api/meetings")
                    .cookie(Cookie("accessToken", token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "팀 회의",
                          "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                          "duration": 60,
                          "category": "PROJECT"
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.meetingId").isNumber)
                .andExpect(jsonPath("$.data.roomUrl").isString)
        }

        @Test
        fun `성공 - 로그인 API에서 발급된 쿠키로도 생성 가능`() {
            val email = "creator-login@example.com"
            val rawPassword = "password123!"
            memberRepository.save(Member(email, passwordEncoder.encode(rawPassword)!!, "creator-login", TimezoneType.ASIA_SEOUL))

            val loginCookie = mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "email": "$email", "password": "$rawPassword" }""")
            )
                .andExpect(status().isOk())
                .andReturn().response.getCookie("accessToken")!!

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
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roomUrl").isString)
        }

        @Test
        fun `실패 - 비로그인 시 401`() {
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
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isUnauthorized())
        }

        @Test
        fun `실패 - 위조 토큰이면 401`() {
            mvc.perform(
                post("/api/meetings")
                    .cookie(Cookie("accessToken", "invalid.jwt.token"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "팀 회의",
                          "dates": ["2026-04-20"],
                          "duration": 60,
                          "category": "PROJECT"
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"))
        }

        @Test
        fun `실패 - 날짜 미입력 시 400`() {
            val token = authTokenHelper.createToken(saveMember("creator2@example.com"))

            mvc.perform(
                post("/api/meetings")
                    .cookie(Cookie("accessToken", token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "팀 회의",
                          "duration": 60,
                          "category": "PROJECT"
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isBadRequest())
        }

        @Test
        fun `실패 - 존재하지 않는 회원이면 404`() {
            val token = authTokenHelper.createTokenWithPayload(mapOf("id" to 999999, "nickname" to "ghost"))

            mvc.perform(
                post("/api/meetings")
                    .cookie(Cookie("accessToken", token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "title": "팀 회의",
                          "dates": ["2026-04-20"],
                          "duration": 60,
                          "category": "PROJECT"
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isNotFound())
        }
    }

    // ── 랜덤 URL 조회 ─────────────────────────────────────────────────────────

    @Nested
    inner class `GET api meetings randomUrl` {

        @Test
        fun `성공 - 200과 모임 상세 정보 반환`() {
            val token = authTokenHelper.createToken(saveMember("creator3@example.com"))
            val roomUrl = getRoomUrl(token)

            mvc.perform(get("/api/meetings/{randomUrl}", roomUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.roomUrl").value(roomUrl))
                .andExpect(jsonPath("$.data.title").value("테스트 모임"))
        }

        @Test
        fun `실패 - 존재하지 않는 URL이면 404`() {
            mvc.perform(get("/api/meetings/{randomUrl}", "notExistsUrl"))
                .andDo(print())
                .andExpect(status().isNotFound())
        }
    }

    // ── 내 모임 목록 조회 ─────────────────────────────────────────────────────

    @Nested
    inner class `GET api meetings` {

        @Test
        fun `성공 - 내가 만든 모임 목록 반환`() {
            val token = authTokenHelper.createToken(saveMember("list@example.com"))
            getRoomUrl(token) // 모임 1개 생성

            mvc.perform(
                get("/api/meetings")
                    .cookie(Cookie("accessToken", token))
            )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].title").value("테스트 모임"))
        }

        @Test
        fun `성공 - 모임이 없으면 빈 목록 반환`() {
            val token = authTokenHelper.createToken(saveMember("empty@example.com"))

            mvc.perform(
                get("/api/meetings")
                    .cookie(Cookie("accessToken", token))
            )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        fun `실패 - 비로그인 시 401`() {
            mvc.perform(get("/api/meetings"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
        }
    }

    // ── 모임 삭제 ─────────────────────────────────────────────────────────────

    @Nested
    inner class `DELETE api meetings meetingId` {

        @Test
        fun `성공 - 방장이 삭제 시 200`() {
            val token = authTokenHelper.createToken(saveMember("delete-host@example.com"))
            val meetingId = getMeetingId(token)

            mvc.perform(
                delete("/api/meetings/{meetingId}", meetingId)
                    .cookie(Cookie("accessToken", token))
            )
                .andDo(print())
                .andExpect(status().isOk())
        }

        @Test
        fun `실패 - 방장이 아닌 회원이 삭제 시도 시 403`() {
            val hostToken = authTokenHelper.createToken(saveMember("delete-host2@example.com"))
            val otherToken = authTokenHelper.createToken(saveMember("delete-other@example.com"))
            val meetingId = getMeetingId(hostToken)

            mvc.perform(
                delete("/api/meetings/{meetingId}", meetingId)
                    .cookie(Cookie("accessToken", otherToken))
            )
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MEETING-002"))
        }

        @Test
        fun `실패 - 비로그인 시 401`() {
            mvc.perform(delete("/api/meetings/{meetingId}", 1))
                .andDo(print())
                .andExpect(status().isUnauthorized())
        }
    }

    // ── 일정 확정 ─────────────────────────────────────────────────────────────

    @Nested
    inner class `POST api meetings meetingId confirm` {

        @Test
        fun `성공 - 참여자 있는 모임 확정 시 200`() {
            val token = authTokenHelper.createToken(saveMember("confirm-host@example.com"))
            val roomUrl = getRoomUrl(token)
            val meetingId = getMeetingIdByUrl(roomUrl)
            addParticipant(roomUrl)

            mvc.perform(
                post("/api/meetings/{meetingId}/confirm", meetingId)
                    .cookie(Cookie("accessToken", token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "date": "2026-04-20", "time": "14:00" }""")
            )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        }

        @Test
        fun `실패 - 참여자 없는 모임 확정 시 400`() {
            val token = authTokenHelper.createToken(saveMember("confirm-empty@example.com"))
            val meetingId = getMeetingId(token)

            mvc.perform(
                post("/api/meetings/{meetingId}/confirm", meetingId)
                    .cookie(Cookie("accessToken", token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "date": "2026-04-20", "time": "14:00" }""")
            )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEETING-005"))
        }

        @Test
        fun `실패 - 방장이 아닌 회원이 확정 시도 시 403`() {
            val hostToken = authTokenHelper.createToken(saveMember("confirm-host2@example.com"))
            val otherToken = authTokenHelper.createToken(saveMember("confirm-other@example.com"))
            val meetingId = getMeetingId(hostToken)

            mvc.perform(
                post("/api/meetings/{meetingId}/confirm", meetingId)
                    .cookie(Cookie("accessToken", otherToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "date": "2026-04-20", "time": "14:00" }""")
            )
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MEETING-002"))
        }

        @Test
        fun `실패 - 비로그인 시 401`() {
            mvc.perform(
                post("/api/meetings/{meetingId}/confirm", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{ "date": "2026-04-20", "time": "14:00" }""")
            )
                .andDo(print())
                .andExpect(status().isUnauthorized())
        }
    }

    // ── 일정 확정 취소 ────────────────────────────────────────────────────────

    @Nested
    inner class `DELETE api meetings meetingId confirm` {

        @Test
        fun `성공 - 확정된 모임 취소 시 200`() {
            val token = authTokenHelper.createToken(saveMember("cancel-host@example.com"))
            val roomUrl = getRoomUrl(token)
            val meetingId = getMeetingIdByUrl(roomUrl)
            addParticipant(roomUrl)
            confirmMeeting(meetingId, token)

            mvc.perform(
                delete("/api/meetings/{meetingId}/confirm", meetingId)
                    .cookie(Cookie("accessToken", token))
            )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
        }

        @Test
        fun `실패 - 방장이 아닌 회원이 취소 시도 시 403`() {
            val hostToken = authTokenHelper.createToken(saveMember("cancel-host2@example.com"))
            val otherToken = authTokenHelper.createToken(saveMember("cancel-other@example.com"))
            val roomUrl = getRoomUrl(hostToken)
            val meetingId = getMeetingIdByUrl(roomUrl)
            addParticipant(roomUrl)
            confirmMeeting(meetingId, hostToken)

            mvc.perform(
                delete("/api/meetings/{meetingId}/confirm", meetingId)
                    .cookie(Cookie("accessToken", otherToken))
            )
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MEETING-002"))
        }
    }

    // ── 확정 일정 조회 ────────────────────────────────────────────────────────

    @Nested
    inner class `GET api meetings meetingId confirm` {

        @Test
        fun `성공 - 확정된 모임의 일정 정보 반환`() {
            val token = authTokenHelper.createToken(saveMember("schedule-host@example.com"))
            val roomUrl = getRoomUrl(token)
            val meetingId = getMeetingIdByUrl(roomUrl)
            addParticipant(roomUrl)
            confirmMeeting(meetingId, token)

            mvc.perform(get("/api/meetings/{meetingId}/confirm", meetingId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.date").value("2026-04-20"))
        }

        @Test
        fun `실패 - 미확정 모임 조회 시 400`() {
            val token = authTokenHelper.createToken(saveMember("schedule-pending@example.com"))
            val meetingId = getMeetingId(token)

            mvc.perform(get("/api/meetings/{meetingId}/confirm", meetingId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEETING-004"))
        }
    }
}
