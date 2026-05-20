package com.back.nbe9112team06.global.response

import com.fasterxml.jackson.annotation.JsonIgnore

data class ApiResponse<T>(
    val resultCode: String,
    val msg: String,
    val data: T? = null
) {
    constructor(msg: String, resultCode: String) : this(resultCode, msg, null)

    //TODO substringBefore로 바꾸긴했지만 추후에 다시 확인 원래는 split으로 나누고 있었다.
    @get:JsonIgnore
    val statusCode: Int
        get() = resultCode
            .substringBefore("-", resultCode)
            .toInt()
}