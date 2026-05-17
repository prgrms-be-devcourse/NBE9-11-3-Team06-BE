package com.back.nbe9112team06.global.springDoc.example

object ParticipantApiExamples {

    const val JOIN_MEETING_SUCCESS_JSON: String = """
            {
              "code": "201-1",
              "message": "모임방 참가 성공",
              "data": {
                "participantId": 1,
                "guestName": "홍길동"
              }
            }          
            """

    const val JOIN_MEETING_VALIDATION_ERROR_JSON: String = """
            {
              "errorCode": "PARTICIPANT-001",
              "detail": "참가자 이름은 필수입니다."
            }           
            """

    const val JOIN_MEETING_NOT_FOUND_JSON: String = """
            {
              "errorCode": "MEETING-001",
              "detail": "존재하지 않는 모임입니다."
            }           
            """
}