package com.back.nbe9112team06.global.exception;

import com.back.nbe9112team06.global.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/business")
    public void business() {
        // ✅ USER_NOT_FOUND → NOT_FOUND 사용
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @GetMapping("/unexpected")
    public void unexpected() {
        throw new RuntimeException("테스트용 예외 - 노출되면 안 됨");
    }

    @PostMapping(value = "/valid", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void valid(@Valid @RequestBody TestDto dto) {}

    @GetMapping("/param-type")
    public void typeMismatch(@RequestParam Integer id) {}

    @GetMapping("/param-missing")
    public void missingParam(@RequestParam String name) {}

    @GetMapping("/validated")
    public void constraintViolation(@RequestParam @NotBlank String code) {}

    @GetMapping("/not-found-test")
    public void notFoundTest() {}

    @PostMapping("/method-test")
    public void methodTest() {}

    @PostMapping(value = "/media-type-test", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void mediaTypeTest(@RequestBody Map<String, Object> body) {}

    @GetMapping("/db-violation")
    public void dbViolation() {
        throw new DataIntegrityViolationException("Duplicate entry for key 'email'");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestDto {
        @NotBlank
        private String name;

        @Email
        private String email;
    }
}