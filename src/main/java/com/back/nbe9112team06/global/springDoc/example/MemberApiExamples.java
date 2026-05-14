package com.back.nbe9112team06.global.springDoc.example;

public class MemberApiExamples {

    // ── POST /api/members (회원가입) ──────────────────────────────

    public static final String SIGNUP_SUCCESS_JSON = """
            {
              "code": "201-1",
              "message": "회원가입에 성공하셨습니다",
              "data": {
                "email": "user@example.com",
                "nickname": "gildong"
              }
            }
            """;

    public static final String SIGNUP_VALIDATION_ERROR_JSON = """
            {
              "errorCode": "COMMON-009",
              "detail": "입력값 검증에 실패했습니다.",
              "validationErrors": [
                {"field": "email", "message": "올바른 이메일 형식이 아닙니다"},
                {"field": "password", "message": "비밀번호는 8자 이상 20자 이하여야 합니다."}
              ]
            }
            """;

    // ── POST /api/members/check-email (이메일 중복 체크) ──────────────────────────────

    public static final String CHECK_EMAIL_AVAILABLE_JSON = """
            {
              "code": "200-1",
              "message": "사용 가능한 이메일입니다.",
              "data": {
                "available": true
              }
            }
            """;

    public static final String CHECK_EMAIL_DUPLICATE_JSON = """
            {
              "code": "200-1",
              "message": "이미 등록된 이메일입니다.",
              "data": {
                "available": false
              }
            }
            """;

    // ── DELETE /api/members (회원 탈퇴) ──────────────────────────────

    public static final String DELETE_MEMBER_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "회원 탈퇴가 완료되었습니다.",
              "data": null
            }
            """;

    // ── 공통 에러 응답 예시 (참고용) ──────────────────────────────

    public static final String MEMBER_NOT_FOUND_JSON = """
            {
              "type": "https://api.nbe9112team06.com/errors/member/001",
              "title": "Not Found",
              "status": 404,
              "detail": "존재하지 않는 회원입니다.",
              "errorCode": "MEMBER-001",
              "timestamp": "2024-01-15T10:30:00Z"
            }
            """;

    public static final String DUPLICATE_EMAIL_JSON = """
            {
              "type": "https://api.nbe9112team06.com/errors/member/002",
              "title": "Conflict",
              "status": 409,
              "detail": "이미 등록된 이메일입니다.",
              "errorCode": "MEMBER-002",
              "timestamp": "2024-01-15T10:30:00Z"
            }
            """;
}