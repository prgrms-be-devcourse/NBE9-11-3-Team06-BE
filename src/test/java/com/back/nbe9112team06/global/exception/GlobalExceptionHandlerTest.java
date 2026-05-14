package com.back.nbe9112team06.global.exception;

import com.back.nbe9112team06.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("필수 필드 5개가 모두 정확히 응답된다 (RFC 9457)")
    void requiredFields_areReturned() throws Exception {
        String expectedCode = ErrorCode.NOT_FOUND.getCode();  // "COMMON-003"

        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/003"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errorCode").value(expectedCode))
                .andExpect(jsonPath("$.instance").value("/test/business"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("traceId 가 요청에 포함되면 응답에도 동일하게 포함된다")
    void traceId_ifPresent_hasValidFormat() throws Exception {
        String testTraceId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

        mockMvc.perform(get("/test/business")
                        .requestAttr("traceId", testTraceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").value(testTraceId));
    }

    @Test
    @DisplayName("검증 실패 시 validationErrors 확장 필드가 포함된다")
    void validationFailure_returnsProblemDetailWithErrors() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/009"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.errorCode").value("COMMON-009"))
                .andExpect(jsonPath("$.instance").value("/test/valid"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[?(@.field=='email')]").exists());
    }

    @Test
    @DisplayName("내부 오류 시 민감 정보가 detail 에 노출되지 않는다")
    void unexpectedException_doesNotExposeSensitiveInfo() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/001"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."))
                .andExpect(jsonPath("$.errorCode").value("COMMON-001"))
                .andExpect(jsonPath("$.instance").value("/test/unexpected"))
                .andExpect(jsonPath("$.detail", not(containsString("테스트용 예외"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.detail", not(containsString("RuntimeException"))));
    }

    @Test
    @DisplayName("타입 변환 실패 시 파라미터 정보가 포함된다")
    void handleTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/param-type")
                        .param("id", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COMMON-006"))
                .andExpect(jsonPath("$.detail", allOf(
                        containsString("타입 변환"),
                        containsString("id")
                )))
                .andExpect(jsonPath("$.parameterName").value("id"))
                .andExpect(jsonPath("$.receivedValue").value("not-a-number"));
    }

    @Test
    @DisplayName("필수 파라미터 누락 시 파라미터 이름이 포함된다")
    void handleMissingParam() throws Exception {
        mockMvc.perform(get("/test/param-missing"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COMMON-007"))
                .andExpect(jsonPath("$.detail", containsString("name")))
                .andExpect(jsonPath("$.parameterName").value("name"));
    }

    @Test
    @DisplayName("파라미터 @NotBlank 검증 실패 시 필드명이 포함된다")
    void handleMethodValidationFailure() throws Exception {
        mockMvc.perform(get("/test/validated")
                        .param("code", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.nbe9112team06.com/errors/common/009"))
                .andExpect(jsonPath("$.detail", allOf(
                        containsString("code"),
                        containsStringIgnoringCase("blank")
                )));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 호출 시 405 응답")
    void handleMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/test/method-test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COMMON-004"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.detail", containsString("지원하지 않는 HTTP 메서드")));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type 요청 시 415 응답")
    void handleUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test/media-type-test")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COMMON-005"))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.detail").value("지원하지 않는 Content-Type 입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 경로 요청 시 404 응답")
    void handleNotFound() throws Exception {
        mockMvc.perform(get("/test/this-path-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COMMON-003"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("찾을 수 없습니다")));
    }

    @Test
    @DisplayName("DB 무결성 위반 시 일반화된 409 응답")
    void handleDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/test/db-violation"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errorCode").value("COMMON-008"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("이미 존재하는 리소스입니다. 또는 데이터 무결성 제약조건에 위반됩니다."))
                .andExpect(jsonPath("$.detail", not(containsString("Duplicate entry"))));
    }
}