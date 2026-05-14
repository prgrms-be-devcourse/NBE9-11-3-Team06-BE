package com.back.nbe9112team06.global.springDoc.example;

public class ParticipantApiExamples {
    public static final String JOIN_MEETING_SUCCESS_JSON = """
            {
              "code": "201-1",
              "message": "모임방 참가 성공",
              "data": {
                "participantId": 1,
                "guestName": "홍길동"
              }
            }
            """;

    public static final String JOIN_MEETING_VALIDATION_ERROR_JSON = """
            {
              "errorCode": "PARTICIPANT-001",
              "detail": "참가자 이름은 필수입니다."
            }
            """;

    public static final String JOIN_MEETING_NOT_FOUND_JSON = """
            {
              "errorCode": "MEETING-001",
              "detail": "존재하지 않는 모임입니다."
            }
            """;
}