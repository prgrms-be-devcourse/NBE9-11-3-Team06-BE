package com.back.nbe9112team06.domain.timetable.controller

import com.back.nbe9112team06.domain.timetable.service.TimeTableService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Sql("/timetable-test-data.sql")
internal class TimeTableLockTest {
    @Autowired
    lateinit var timeTableService: TimeTableService

    @Test
    @Throws(Exception::class)
    fun 비관적락_대기_확인() {
        val executor =
            Executors.newFixedThreadPool(2)

        val latch = CountDownLatch(2)

        val start = System.currentTimeMillis()

        val task = Runnable {
            try {
                timeTableService.aggregate(1)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                latch.countDown()
            }
        }

        executor.submit(task)

        Thread.sleep(100)

        executor.submit(task)

        latch.await()

        val end = System.currentTimeMillis()

        println("총 실행 시간 = " + (end - start))
    }
}
