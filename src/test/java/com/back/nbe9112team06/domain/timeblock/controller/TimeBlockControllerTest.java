package com.back.nbe9112team06.domain.timeblock.controller;

import com.back.nbe9112team06.domain.timeblock.service.TimeBlockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/* 등록 API
    정상 등록 201
    형식 오류 400
    필드 누락 400
*/
/* 삭제 API
    정상 삭제 204
*/

@WebMvcTest(TimeBlockController.class)
@AutoConfigureDataJpa
@MockitoBean(types = JpaMetamodelMappingContext.class)
public class TimeBlockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeBlockService timeBlockService;

    @Test
    @WithMockUser
    void 시간표등록() throws Exception {
        // given
        Integer meetingId = 1;
        String requestBody = """
                {
                    "guestName": "김아무개",
                    "guestPassword": "1234",
                    "availableDateTimes": ["2026-04-20 10:00", "2026-04-20 10:30"]
                }
                """;
        //when
        mockMvc.perform(

                post("/api/meetings/{meetingId}/time-blocks", meetingId)
                        .with(csrf()) // csrf 비활성화
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                // then
                .andExpect(status().isCreated());
    }

    // getName이 없을 때
    @Test
    @WithMockUser
    void getName필드누락() throws Exception {
        Integer meetingId = 1;
        String requestBody = """
                {
                    "guestPassword": "1234",
                    "availableDateTimes": ["2026-04-20 10:00", "2026-04-20 10:30"]
                }
                """;
        //when
        mockMvc.perform(

                        post("/api/meetings/{meetingId}/time-blocks", meetingId)
                                .with(csrf()) // csrf 비활성화
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                // then
                .andExpect(status().isBadRequest());
    }

    // availableDateTimes이 없을 때
    @Test
    @WithMockUser
    void availableDateTimes필드누락() throws Exception {
        Integer meetingId = 1;
        String requestBody = """
                {
                    "guestName": "김아무개",
                    "guestPassword": "1234"
                }
                """;
        //when
        mockMvc.perform(

                        post("/api/meetings/{meetingId}/time-blocks", meetingId)
                                .with(csrf()) // csrf 비활성화
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                // then
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void 정상삭제() throws Exception {
        //given
        Integer meetingId = 1;
        String requestBody = """
                {
                "guestName": "김아무개",
                "guestPassword": "1234"
                }
                """;

        //when
        mockMvc.perform(
                delete("/api/meetings/{meetingId}/time-blocks", meetingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                //then
                .andExpect(status().isNoContent());

    }

}


