package com.back.nbe9112team06.global.springDoc.example;

public class AuthApiExamples {

    // ── POST /api/auth/login ──────────────────────────────

    public static final String LOGIN_SUCCESS_JSON = """
            {
              "code": "201-1",
              "message": "로그인 성공",
              "data": {
                "nickname": "gildong"
              }
            }
            """;

    // ── GET /api/auth/me ──────────────────────────────

    public static final String GET_MY_INFO_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "조회 성공",
              "data": {
                "nickname": "gildong"
              }
            }
            """;

    // ── POST /api/auth/logout ──────────────────────────────

    public static final String LOGOUT_SUCCESS_JSON = """
            {
              "code": "200-1",
              "message": "로그아웃 성공",
              "data": null
            }
            """;
}