package com.back.nbe9112team06.domain.meeting.dto.response;

import com.back.nbe9112team06.domain.meeting.entity.MeetingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MeetingEntryResponse(
        Integer meetingId,
        String title,
        String category,
        Integer duration,
        MeetingStatus status,
        String roomUrl,
        List<LocalDate> dates,
        LocalDateTime createdAt,
        LocalDate confirmedDate,
        LocalTime confirmedTime
) {
}

