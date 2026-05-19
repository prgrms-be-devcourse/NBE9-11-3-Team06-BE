package com.back.nbe9112team06.domain.timeblock.controller

import com.back.nbe9112team06.domain.timeblock.service.TimeBlockService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/* 등록 API
    정상 등록 201
    형식 오류 400
    필드 누락 400
*/
/* 삭제 API
    정상 삭제 204
*/
@WebMvcTest(TimeBlockController::class)
@MockitoBean(types = [JpaMetamodelMappingContext::class])
class TimeBlockControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var timeBlockService: TimeBlockService

    @Test
    @WithMockUser
    fun 시간표등록() {
        // given
        val meetingId = 1
        val requestBody = """
                {
                    "guestName": "김아무개",
                    "guestPassword": "1234",
                    "availableDateTimes": ["2026-04-20 10:00", "2026-04-20 10:30"]
                }
                
                """
        //when
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/meetings/{meetingId}/time-blocks", meetingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf 비활성화
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ) // then
            .andExpect(MockMvcResultMatchers.status().isCreated())
    }

    // getName이 없을 때
    @Test
    @WithMockUser
    fun getName필드누락() {
        val meetingId = 1
        val requestBody = """
                {
                    "guestPassword": "1234",
                    "availableDateTimes": ["2026-04-20 10:00", "2026-04-20 10:30"]
                }
                
                """
        //when
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/meetings/{meetingId}/time-blocks", meetingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf 비활성화
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ) // then
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    // availableDateTimes이 없을 때
    @Test
    @WithMockUser
    fun availableDateTimes필드누락() {
        val meetingId = 1
        val requestBody = """
                {
                    "guestName": "김아무개",
                    "guestPassword": "1234"
                }
                
                """
        //when
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/meetings/{meetingId}/time-blocks", meetingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf()) // csrf 비활성화
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ) // then
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    @Test
    @WithMockUser
    fun 정상삭제() {
        //given
        val meetingId = 1
        val requestBody = """
                {
                "guestName": "김아무개",
                "guestPassword": "1234"
                }
                
                """

        //when
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/meetings/{meetingId}/time-blocks", meetingId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ) //then
            .andExpect(MockMvcResultMatchers.status().isNoContent())
    }
}