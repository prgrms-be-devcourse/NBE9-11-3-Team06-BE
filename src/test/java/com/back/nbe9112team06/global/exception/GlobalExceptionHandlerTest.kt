package com.back.nbe9112team06.global.exception

import com.back.nbe9112team06.global.error.ErrorCode
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GlobalExceptionHandler 통합 테스트")
internal class GlobalExceptionHandlerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("필수 필드 5개가 모두 정확히 응답된다 (RFC 9457)")
    fun requiredFields_areReturned() {
        val expectedCode = ErrorCode.NOT_FOUND.code // "COMMON-003"

        mockMvc.perform(MockMvcRequestBuilders.get("/test/business"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/003")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Not Found"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("요청한 리소스를 찾을 수 없습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value(expectedCode))
            .andExpect(MockMvcResultMatchers.jsonPath("$.instance").value("/test/business"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
    }

    @Test
    @DisplayName("traceId 가 요청에 포함되면 응답에도 동일하게 포함된다")
    fun traceId_ifPresent_hasValidFormat() {
        val testTraceId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

        mockMvc.perform(
            MockMvcRequestBuilders.get("/test/business")
                .requestAttr("traceId", testTraceId)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(testTraceId))
    }

    @Test
    @DisplayName("검증 실패 시 validationErrors 확장 필드가 포함된다")
    fun validationFailure_returnsProblemDetailWithErrors() {
        val invalidJson = """
                {
                    "name": "",
                    "email": "not-an-email"
                }
                
                """.trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/test/valid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/009")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-009"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.instance").value("/test/valid"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.validationErrors[?(@.field=='email')]").exists())
    }

    @Test
    @DisplayName("내부 오류 시 민감 정보가 detail 에 노출되지 않는다")
    fun unexpectedException_doesNotExposeSensitiveInfo() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/unexpected"))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/001")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Internal Server Error"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-001"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.instance").value("/test/unexpected"))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.detail",
                    Matchers.not(Matchers.containsString("테스트용 예외"))
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.stackTrace").doesNotExist())
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.detail",
                    Matchers.not(Matchers.containsString("RuntimeException"))
                )
            )
    }

    @Test
    @DisplayName("타입 변환 실패 시 파라미터 정보가 포함된다")
    fun handleTypeMismatch() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/test/param-type")
                .param("id", "not-a-number")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-006"))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.detail", Matchers.allOf(
                        Matchers.containsString("타입 변환"),
                        Matchers.containsString("id")
                    )
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.parameterName").value("id"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.receivedValue").value("not-a-number"))
    }

    @Test
    @DisplayName("필수 파라미터 누락 시 파라미터 이름이 포함된다")
    fun handleMissingParam() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/param-missing"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-007"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.containsString("name")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.parameterName").value("name"))
    }

    @Test
    @DisplayName("파라미터 @NotBlank 검증 실패 시 필드명이 포함된다")
    fun handleMethodValidationFailure() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/test/validated")
                .param("code", "  ")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/009")
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.detail", Matchers.allOf(
                        Matchers.containsString("code"),
                        Matchers.containsStringIgnoringCase("blank")
                    )
                )
            )
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 호출 시 405 응답")
    fun handleMethodNotAllowed() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/method-test"))
            .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-004"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(405))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.containsString("지원하지 않는 HTTP 메서드")))
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type 요청 시 415 응답")
    fun handleUnsupportedMediaType() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/test/media-type-test")
                .contentType(MediaType.TEXT_PLAIN)
                .content("not-json")
        )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-005"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(415))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("지원하지 않는 Content-Type 입니다."))
    }

    @Test
    @DisplayName("존재하지 않는 경로 요청 시 404 응답")
    fun handleNotFound() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/this-path-does-not-exist"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-003"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.containsString("찾을 수 없습니다")))
    }

    @Test
    @DisplayName("DB 무결성 위반 시 일반화된 409 응답")
    fun handleDataIntegrityViolation() {
        mockMvc.perform(MockMvcRequestBuilders.get("/test/db-violation"))
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("COMMON-008"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("이미 존재하는 리소스입니다. 또는 데이터 무결성 제약조건에 위반됩니다."))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.detail",
                    Matchers.not(Matchers.containsString("Duplicate entry"))
                )
            )
    }
}