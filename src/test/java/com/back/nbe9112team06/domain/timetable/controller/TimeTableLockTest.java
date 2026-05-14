package com.back.nbe9112team06.domain.timetable.controller;

import com.back.nbe9112team06.domain.timetable.service.TimeTableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Sql("/timetable-test-data.sql")
class TimeTableLockTest {

    @Autowired
    TimeTableService timeTableService;

    @Test
    void 비관적락_대기_확인() throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch latch = new CountDownLatch(2);

        long start = System.currentTimeMillis();

        Runnable task = () -> {
            try {
                timeTableService.aggregate(1);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };

        executor.submit(task);

        Thread.sleep(100);

        executor.submit(task);

        latch.await();

        long end = System.currentTimeMillis();

        System.out.println("총 실행 시간 = " + (end - start));
    }
}
