package com.back.nbe9112team06.global.springDoc.example

object AuthApiExamples {
    // ── POST /api/auth/login ──────────────────────────────
    const val LOGIN_SUCCESS_JSON: String = """
            {
              "code": "201-1",
              "message": "로그인 성공",
              "data": {
                "nickname": "gildong"
              }
            }
            """

    // ── GET /api/auth/me ──────────────────────────────
    const val GET_MY_INFO_SUCCESS_JSON: String = """
            {
              "code": "200-1",
              "message": "조회 성공",
              "data": {
                "nickname": "gildong"
              }
            }
            """

    // ── POST /api/auth/logout ──────────────────────────────
    const val LOGOUT_SUCCESS_JSON: String = """
            {
              "code": "200-1",
              "message": "로그아웃 성공",
              "data": null
            }
            """
}