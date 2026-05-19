package com.back.nbe9112team06.domain.participant.controller

import com.back.nbe9112team06.domain.member.entity.Member
import com.back.nbe9112team06.domain.member.entity.TimezoneType
import com.back.nbe9112team06.domain.member.repository.MemberRepository
import com.back.nbe9112team06.testutil.AuthTokenHelper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ParticipantControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var authTokenHelper: AuthTokenHelper

    @Test
    fun `모임방 참가 - 성공 시 201과 participantId를 반환한다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator1@example.com", "creator1")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "홍길동",
                      "guestPassword": "1234"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.msg").value("모임방 참가 성공"))
            .andExpect(jsonPath("$.data.participantId").isNumber())
            .andExpect(jsonPath("$.data.guestName").value("홍길동"))
    }

    @Test
    fun `모임방 참가 - 존재하지 않는 randomUrl이면 404를 반환한다`() {
        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", "notExistsUrl")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "홍길동",
                      "guestPassword": "1234"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isNotFound())
    }

    @Test
    fun `모임방 참가 - 같은 이름으로 중복 참가해도 모두 성공한다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator2@example.com", "creator2")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "중복이름",
                      "guestPassword": "1111"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isCreated())

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "중복이름",
                      "guestPassword": "2222"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.guestName").value("중복이름"))
    }

    @Test
    fun `모임방 참가 - 이름과 비밀번호가 모두 동일한 참가자가 이미 존재하면 409를 반환한다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator7@example.com", "creator7")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "홍길동",
                      "guestPassword": "1234"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated())

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "홍길동",
                      "guestPassword": "1234"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isConflict())
    }

    @Test
    fun `모임방 참가 - 이름 또는 비밀번호가 공백이면 400을 반환한다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator3@example.com", "creator3")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "",
                      "guestPassword": "1234"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `모임방 참가 - guestName 필드 자체가 없으면 400을 반환한다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator5@example.com", "creator5")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestPassword": "1234"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `모임방 참가 - guestPassword 필드 자체가 없으면 400을 반환한다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator6@example.com", "creator6")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "홍길동"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun `모임방 참가 - 유효하지 않은 토큰 쿠키가 있어도 permitAll로 참가 가능하다`() {
        val roomUrl = createMeetingAndGetRoomUrl("creator4@example.com", "creator4")

        mvc.perform(
            post("/api/meetings/{randomUrl}/participants", roomUrl)
                .cookie(Cookie("accessToken", "invalid.jwt.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "guestName": "비회원참가자",
                      "guestPassword": "9999"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.guestName").value("비회원참가자"))
    }

    private fun createMeetingAndGetRoomUrl(email: String, nickname: String): String {
        val member = memberRepository.save(Member(email, "hashedPassword", nickname, TimezoneType.ASIA_SEOUL))
        val token = authTokenHelper.createToken(member)

        val createResponse = mvc.perform(
            post("/api/meetings")
                .cookie(Cookie("accessToken", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "참가 테스트 모임",
                      "dates": ["2026-04-20", "2026-04-21", "2026-04-22"],
                      "duration": 60,
                      "category": "PROJECT"
                    }
                """.trimIndent())
        )
            .andExpect(status().isCreated())
            .andReturn()
            .response
            .contentAsString

        return createResponse.substringAfter("\"roomUrl\":\"").substringBefore("\"")
    }
}
