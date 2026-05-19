package com.back.nbe9112team06

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class Nbe9112Team06Application
    fun main(args: Array<String>) {
        runApplication<Nbe9112Team06Application>(*args)
    }

