package com.back.nbe9112team06.global.springDoc.example;

public class MeetingApiExamples {

    // ── GET /api/meetings ──────────────────────────────
    public static final String GET_MY_MEETINGS_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "모임 목록 조회 성공",
              "data": [
                {
                  "meetingId": 1,
                  "title": "팀 회의",
                  "category": "PROJECT",
                  "duration": 60,
                  "status": "PENDING",
                  "roomUrl": "abc123XYZ",
                  "dates": ["2026-04-20", "2026-04-21"],
                  "createdAt": "2026-04-01T10:00:00",
                  "confirmedDate": null,
                  "confirmedTime": null
                }
              ]
            }
            """;

    // ── POST /api/meetings ──────────────────────────────
    public static final String CREATE_MEETING_SUCCESS_JSON = """
            {
              "code": "201-1",
              "message": "모임방 생성 성공",
              "data": {
                "meetingId": 1,
                "roomUrl": "abc123XYZ"
              }
            }
            """;

    // ── GET /api/meetings/{randomUrl} ──────────────────────────────
    public static final String GET_MEETING_BY_URL_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "모임방 조회 성공",
              "data": {
                "meetingId": 1,
                "title": "팀 회의",
                "category": "PROJECT",
                "duration": 60,
                "status": "PENDING",
                "roomUrl": "abc123XYZ",
                "dates": ["2026-04-20", "2026-04-21"],
                "createdAt": "2026-04-01T10:00:00",
                "confirmedDate": null,
                "confirmedTime": null
              }
            }
            """;

    // ── GET /api/meetings/{randomUrl}/check-creator ──────────────────────────────
    public static final String CHECK_CREATOR_IS_HOST_JSON = """
            {"code":"200-1","message":"방장이 맞습니다.","data":{"isHost":true}}
            """;

    public static final String CHECK_CREATOR_NOT_HOST_JSON = """
            {"code":"200-1","message":"방장이 아닙니다.","data":{"isHost":false}}
            """;

    // ── DELETE /api/meetings/{meetingId} ──────────────────────────────
    public static final String DELETE_MEETING_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "모임이 삭제되었습니다.",
              "data": null
            }
            """;

    // ── POST /api/meetings/{meetingId}/confirm ──────────────────────────────
    public static final String CONFIRM_SCHEDULE_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "일정이 확정되었습니다.",
              "data": {
                "date": "2026-04-20",
                "time": "14:00",
                "message": "📅 팀 회의 일정이 확정되었습니다!\\n• 날짜: 2026-04-20\\n• 시간: 14:00 ~ 15:00",
                "status": "CONFIRMED"
              }
            }
            """;

    // ── DELETE /api/meetings/{meetingId}/confirm ──────────────────────────────
    public static final String CANCEL_CONFIRM_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "일정 확정이 취소되었습니다.",
              "data": null
            }
            """;

    // ── GET /api/meetings/{meetingId}/confirm ──────────────────────────────
    public static final String GET_CONFIRMED_SCHEDULE_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "확정된 일정입니다.",
              "data": {
                "date": "2026-04-20",
                "time": "14:00",
                "message": "📅 팀 회의 일정이 확정되었습니다!\\n• 날짜: 2026-04-20\\n• 시간: 14:00 ~ 15:00",
                "status": "CONFIRMED"
              }
            }
            """;
}