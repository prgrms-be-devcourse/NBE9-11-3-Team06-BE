package com.back.nbe9112team06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Nbe9112Team06Application {

    public static void main(String[] args) {
        SpringApplication.run(Nbe9112Team06Application.class, args);
    }

}
