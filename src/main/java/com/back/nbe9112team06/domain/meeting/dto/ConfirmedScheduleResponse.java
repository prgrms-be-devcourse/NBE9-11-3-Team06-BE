package com.back.nbe9112team06.domain.meeting.dto;

import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record ConfirmedScheduleResponse(LocalDate date, LocalTime time, String message, MeetingStatus status) {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static ConfirmedScheduleResponse from(LocalDate date, LocalTime time, MeetingStatus status, String title, Integer duration){
        String endTime = time.plusMinutes(duration).format(TIME_FMT);
        String message = String.format(
                "📅 %s 일정이 확정되었습니다!\n\n• 날짜: %s\n• 시간: %s ~ %s",
                title, date, time.format(TIME_FMT), endTime
        );
        return new ConfirmedScheduleResponse(date, time, message, status);
    }
}
