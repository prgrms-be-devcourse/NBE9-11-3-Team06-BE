package com.back.nbe9112team06.global.springDoc.example;

public class TimeBlockApiExamples {

    // ── POST /api/meetings/{meetingId}/time-blocks ──────────────────────────────
    public static final String ADD_TIMEBLOCK_SUCCESS_JSON = """
            {
              "code": "201-1",
              "message": "시간표가 등록되었습니다.",
              "data": null
            }
            """;

    public static final String ADD_TIMEBLOCK_VALIDATION_ERROR_JSON = """
            {
              "errorCode": "TIMEBLOCK-001",
              "detail": "올바른 날짜 형식이 아닙니다. (yyyy-MM-dd HH:mm)"
            }
            """;

    // ── DELETE /api/meetings/{meetingId}/time-blocks ──────────────────────────────
    public static final String DELETE_TIMEBLOCK_SUCCESS_JSON = """
            {
              "code": "204-1",
              "message": "시간표가 삭제되었습니다.",
              "data": null
            }
            """;

    // ── GET /api/meetings/{meetingId}/participants ──────────────────────────────
    public static final String GET_PARTICIPANT_SCHEDULES_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "참여자 목록입니다.",
              "data": [
                {
                  "name": "김철수",
                  "availableTimeRanges": [
                    {
                      "date": "2026-04-20",
                      "startTime": "14:00",
                      "endTime": "15:30"
                    },
                    {
                      "date": "2026-04-21",
                      "startTime": "10:00",
                      "endTime": "12:00"
                    }
                  ]
                },
                {
                  "name": "이영희",
                  "availableTimeRanges": [
                    {
                      "date": "2026-04-20",
                      "startTime": "14:30",
                      "endTime": "16:00"
                    }
                  ]
                }
              ]
            }
            """;
}