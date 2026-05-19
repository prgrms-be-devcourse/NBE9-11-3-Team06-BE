package com.back.nbe9112team06.global.exception

import com.back.nbe9112team06.global.error.ErrorCode
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/test")
internal class TestController {
    @GetMapping("/business")
    fun business() {
        // ✅ USER_NOT_FOUND → NOT_FOUND 사용
        throw BusinessException(ErrorCode.NOT_FOUND)
    }

    @GetMapping("/unexpected")
    fun unexpected() {
        throw RuntimeException("테스트용 예외 - 노출되면 안 됨")
    }

    @PostMapping(value = ["/valid"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun valid(@Valid @RequestBody dto: @Valid TestDto) {
    }

    @GetMapping("/param-type")
    fun typeMismatch(@RequestParam id: Int) {
    }

    @GetMapping("/param-missing")
    fun missingParam(@RequestParam name: String) {
    }

    @GetMapping("/validated")
    fun constraintViolation(@RequestParam @NotBlank code: @NotBlank String) {
    }

    @GetMapping("/not-found-test")
    fun notFoundTest() {
    }

    @PostMapping("/method-test")
    fun methodTest() {
    }

    @PostMapping(value = ["/media-type-test"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun mediaTypeTest(@RequestBody body: MutableMap<String, Any>?) {
    }

    @GetMapping("/db-violation")
    fun dbViolation() {
        throw DataIntegrityViolationException("Duplicate entry for key 'email'")
    }

    internal class TestDto(
        @NotBlank private var name: String,
        @Email private var email: String
    )
}