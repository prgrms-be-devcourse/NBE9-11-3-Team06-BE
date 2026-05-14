package com.back.nbe9112team06.global.security;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JsonMapperBuilderCustomizer problemDetailMixinCustomizer() {
        return builder -> {
            //  ProblemDetail 에 Mixin 적용 (properties 평탄화)
            builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
        };
    }
}
