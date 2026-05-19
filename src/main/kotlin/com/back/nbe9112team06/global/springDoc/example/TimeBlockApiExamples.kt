package com.back.nbe9112team06.global.springDoc.example

object TimeBlockApiExamples {
    // ── POST /api/meetings/{meetingId}/time-blocks ──────────────────────────────
    const val  ADD_TIMEBLOCK_SUCCESS_JSON: String = """
            {
              "code": "201-1",
              "message": "시간표가 등록되었습니다.",
              "data": null
            }           
            """

    const val  ADD_TIMEBLOCK_VALIDATION_ERROR_JSON: String = """
            {
              "errorCode": "TIMEBLOCK-001",
              "detail": "올바른 날짜 형식이 아닙니다. (yyyy-MM-dd HH:mm)"
            }           
            """

    // ── DELETE /api/meetings/{meetingId}/time-blocks ──────────────────────────────
    const val  DELETE_TIMEBLOCK_SUCCESS_JSON: String = """
            {
              "code": "204-1",
              "message": "시간표가 삭제되었습니다.",
              "data": null
            }           
            """

    // ── GET /api/meetings/{meetingId}/participants ──────────────────────────────
    const val  GET_PARTICIPANT_SCHEDULES_SUCCESS_JSON: String = """
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
            """
}