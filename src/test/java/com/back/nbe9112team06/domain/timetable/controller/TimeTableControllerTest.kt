package com.back.nbe9112team06.domain.timetable.controller

import com.back.nbe9112team06.domain.timetable.dto.DateResponse
import com.back.nbe9112team06.domain.timetable.dto.RecommendedScheduleResponse
import com.back.nbe9112team06.domain.timetable.dto.TimeResponse
import com.back.nbe9112team06.domain.timetable.dto.TimeTableResponse
import com.back.nbe9112team06.domain.timetable.service.TimeTableService
import com.back.nbe9112team06.global.error.ErrorCode
import com.back.nbe9112team06.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalTime


@WebMvcTest(TimeTableController::class)
@AutoConfigureDataJpa
@MockitoBean(types = [JpaMetamodelMappingContext::class])
class TimeTableControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var timeTableService: TimeTableService

    @Test
    @WithMockUser
    fun 타임테이블_조회_성공() {

        // given
        val response = TimeTableResponse(
            availableDateTimes = listOf(
                DateResponse(
                    availableDate = LocalDate.of(2026, 5, 30),
                    availableTimeInfos = listOf(
                        TimeResponse(
                            time = LocalTime.of(14, 0),
                            participants = listOf("철수", "영희"),
                            count = 2
                        )
                    )
                )
            )
        )

        given(timeTableService.getTimeTable(1))
            .willReturn(response)

        // when & then
        mockMvc.perform(
            get("/api/meetings/{meetingId}/timetable", 1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("타임테이블 조회 성공"))
            .andExpect(
                jsonPath(
                    "$.data.availableDateTimes[0].availableDate"
                ).value("2026-05-30")
            )
            .andExpect(
                jsonPath(
                    "$.data.availableDateTimes[0].availableTimeInfos[0].count"
                ).value(2)
            )
    }

    @Test
    @WithMockUser
    fun 추천일정_조회_성공() {

        // given
        val response = listOf(
            RecommendedScheduleResponse(
                date = LocalDate.of(2026, 5, 30),
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                availableCount = 3
            )
        )

        given(timeTableService.recommend(1))
            .willReturn(response)

        // when & then
        mockMvc.perform(
            get("/api/meetings/{meetingId}/recommend", 1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.msg").value("추천 일정입니다."))
            .andExpect(jsonPath("$.data[0].availableCount").value(3))
    }

    @Test
    @WithMockUser
    fun 존재하지않는_모임_타임테이블조회_실패() {

        // given
        given(timeTableService.getTimeTable(999))
            .willThrow(
                BusinessException(ErrorCode.MEETING_NOT_FOUND)
            )

        // when & then
        mockMvc.perform(
            get("/api/meetings/{meetingId}/timetable", 999)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }
}
