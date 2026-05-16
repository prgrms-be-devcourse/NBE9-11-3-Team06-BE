package com.back.nbe9112team06.global.exception

import com.back.nbe9112team06.global.error.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
/***
 *  getErrorCode()가 val 자동생성된 getErrorCode()과
 *  errorCode에 있는 Code을 가져오는 getErrorCode()와 2가지가 있었어 문제 발생
 *  private로 공개 getter 차단
***/

class BusinessException(
    private val errorCode: ErrorCode,
    customMessage: String?
) : RuntimeException(customMessage) {
    constructor(errorCode: ErrorCode): this(errorCode, null)


    fun getErrorCode(): String = errorCode.code

    val httpStatus: HttpStatus
        get() = errorCode.status

    override val message: String?
        get() = super.message ?: errorCode.message

    /**
     * ProblemDetail 로 변환 (GlobalExceptionHandler 에서 사용)
     */
    fun toProblemDetail(path: String): ProblemDetail {
        val detail = message ?: errorCode.message
        return errorCode.toProblemDetail(detail, path)
    }
}