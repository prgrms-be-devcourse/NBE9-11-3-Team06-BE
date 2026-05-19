package com.back.nbe9112team06.global.security

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.json.JsonMapper

@Configuration
class BeanConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun problemDetailMixinCustomizer(): JsonMapperBuilderCustomizer {
        return JsonMapperBuilderCustomizer { builder: JsonMapper.Builder ->
            //  ProblemDetail 에 Mixin 적용 (properties 평탄화)
            builder.addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java)
        }
    }
}
