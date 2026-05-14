package com.back.nbe9112team06.domain.timetable.controller;

import com.back.nbe9112team06.domain.timetable.dto.TimeTableResponse;
import com.back.nbe9112team06.domain.timetable.service.TimeTableService;
import com.back.nbe9112team06.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
@Sql("/timetable-test-data.sql")
class TimeTableControllerTest {

    @Autowired
    TimeTableService timeTableService;


    @Test
    void aggregate_1번방_정확한_결과_검증() {

        // when
        timeTableService.aggregate(1);
        TimeTableResponse result = timeTableService.getTimeTable(1);


        // then
        assertThat(result.availableDateTimes()).hasSize(1);

        var date = result.availableDateTimes().get(0);
        assertThat(date.availableDate()).isEqualTo("2024-05-20");

        var times = date.availableTimeInfos();

        // 시간별 검증 (정렬 가정)
        assertThat(times).hasSize(3);

        // 09:00 → 철수, 영희
        assertThat(times.get(0).time()).isEqualTo("09:00:00");
        assertThat(times.get(0).participants())
                .containsExactlyInAnyOrder("철수", "영희");

        // 10:00 → 철수
        assertThat(times.get(1).time()).isEqualTo("10:00:00");
        assertThat(times.get(1).participants())
                .containsExactly("철수");

        // 11:00 → 민수
        assertThat(times.get(2).time()).isEqualTo("11:00:00");
        assertThat(times.get(2).participants())
                .containsExactly("민수");

    }

    @Test
    void aggregate_2번방_날짜별_검증() {

        timeTableService.aggregate(2);
        TimeTableResponse result = timeTableService.getTimeTable(2);

        assertThat(result.availableDateTimes()).hasSize(2);

        var first = result.availableDateTimes().get(0);
        var second = result.availableDateTimes().get(1);

        // 날짜 검증
        assertThat(first.availableDate()).isEqualTo("2024-05-21");
        assertThat(second.availableDate()).isEqualTo("2024-05-22");

        // 시간 + 참가자
        assertThat(first.availableTimeInfos().get(0).time()).isEqualTo("10:00:00");
        assertThat(first.availableTimeInfos().get(0).participants())
                .containsExactly("지훈");

        assertThat(second.availableTimeInfos().get(0).participants())
                .containsExactly("수지");
    }

    @Test
    void aggregate_3번방_완전겹침_검증() {

        timeTableService.aggregate(3);
        TimeTableResponse result = timeTableService.getTimeTable(3);

        var date = result.availableDateTimes().get(0);
        var time = date.availableTimeInfos().get(0);

        assertThat(time.time()).isEqualTo("09:00:00");
        assertThat(time.participants())
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void 존재하지않는_모임_aggregate_시_예외발생() {

        assertThatThrownBy(() -> timeTableService.aggregate(999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않는 모임입니다"); // 메시지 맞게 조정
    }

    @Test
    void 타임블럭없으면_aggregate_시_예외발생() {

        // 타임블럭 없는 meetingId
        Integer meetingId = 10;

        assertThatThrownBy(() -> timeTableService.aggregate(meetingId))
                .isInstanceOf(BusinessException.class);
    }
}
