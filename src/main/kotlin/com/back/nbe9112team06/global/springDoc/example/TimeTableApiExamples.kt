package com.back.nbe9112team06.global.springDoc.example

object TimeTableApiExamples {
    // ── GET /api/meetings/{meetingId}/timetable ──────────────────────────────
    const val  GET_TIMETABLE_SUCCESS_JSON: String = """
            {
              "code": "200-1",
              "message": "타임테이블 조회 성공",
              "data": {
                "availableDateTimes": [
                  {
                    "availableDate": "2024-05-20",
                    "availableTimeInfos": [
                      {
                        "time": "09:00",
                        "participants": ["철수", "영희"],
                        "count": 2
                      },
                      {
                        "time": "10:00",
                        "participants": ["철수"],
                        "count": 1
                      },
                      {
                        "time": "11:00",
                        "participants": ["민수"],
                        "count": 1
                      }
                    ]
                  }
                ]
              }
            }
            """


    const val  GET_TIMETABLE_EMPTY_JSON: String = """
            {
              "code": "200-1",
              "message": "타임테이블 조회 성공",
              "data": {
                "availableDateTimes": []
              }
            }
            """

    // ── GET /api/meetings/{meetingId}/recommend ──────────────────────────────
    const val  GET_RECOMMEND_SUCCESS_JSON: String = """
            {
              "code": "200-1",
              "message": "추천 일정입니다.",
              "data": [
                {
                  "date": "2024-05-20",
                  "startTime": "09:00",
                  "endTime": "10:00",
                  "availableCount": 2
                },
                {
                  "date": "2024-05-20",
                  "startTime": "14:00",
                  "endTime": "15:00",
                  "availableCount": 2
                }
              ]
            }
            """
}