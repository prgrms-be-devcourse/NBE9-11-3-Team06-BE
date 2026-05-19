package com.back.nbe9112team06.global.exception

import com.back.nbe9112team06.global.error.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}
    // ✅ 비즈니스 예외 처리 (최우선)
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn { "[BUSINESS_ERROR] code=${ex.getErrorCode()}, message=${ex.message}, path=${request.requestURI}" }

        val pd = ex.toProblemDetail(request.requestURI)

      request.getAttribute("traceId")?.let{
          pd.setProperty("traceId", it.toString())
      }

        return pd
    }

    // ✅ @Valid 검증 실패 처리 (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ProblemDetail {
        val detail = ex.bindingResult.fieldErrors
            .map { it.defaultMessage }  // ← null 도 그대로 포함 (Java 와 동일)
            .filterNotNull()            // ← null 제거 (joinToString 전 처리)
            .joinToString(", ")

        logger.warn {"[VALIDATION_ERROR] detail=${detail}, path=${request.requestURI}"}

        val pd = ErrorCode.VALIDATION_FAILED.toProblemDetail(detail, request.requestURI)

        // ✅ validationErrors 확장 필드 추가
        val errors = ex.bindingResult.fieldErrors.map { err ->
            mapOf(
                "field" to err.field,              // ← Java: err.getField()
                "message" to err.defaultMessage    // ← Java: err.getDefaultMessage() (null 허용)
            )
        }
        pd.setProperty("validationErrors", errors)

        return pd
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidationException(
        ex: HandlerMethodValidationException,
        request: HttpServletRequest
    ): ProblemDetail {
        // ✅ getParameterValidationResults() 사용 (문서 참조)

        val detail = ex.parameterValidationResults.flatMap{ result ->
                // 파라미터 이름 추출 (예: "code")
                val paramName = result.methodParameter.parameterName ?: "unknown"
                result.resolvableErrors.map { error ->
                        val message = error.defaultMessage ?: "유효성 검사 오류"
                             "$paramName: $message"
                }
        }.joinToString(", ")

        logger.warn{"[PARAM_VALIDATION_ERROR] detail=$detail, path=${request.requestURI}"}

        val pd = ErrorCode.VALIDATION_FAILED.toProblemDetail(detail, request.requestURI)

        // ✅ validationErrors 확장 필드 추가 (선택사항, 디버깅용)
        // ✅ validationErrors 확장 필드: 필드명은 "field" 로 통일 (Java 호환)
        val errors = ex.parameterValidationResults.flatMap { result ->
            val paramName = result.methodParameter.parameterName ?: "unknown"
            result.resolvableErrors.map { error ->
                        mapOf(
                            "field" to paramName,
                            "message" to  (error.defaultMessage ?: "유효성 검사 오류")
                        )
            }
        }
        pd.setProperty("validationErrors", errors)

        return pd
    }

    // ✅ 404 Not Found
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFoundException(
        ex: NoResourceFoundException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn {"[NOT_FOUND] path=${request.requestURL}"}

        return ErrorCode.NOT_FOUND.toProblemDetail(
            "요청한 리소스를 찾을 수 없습니다.",
            request.requestURI
        )
    }

    // ✅ 타입 변환 실패 (MethodArgumentTypeMismatchException)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn{"[TYPE_MISMATCH] param=${ex.name}, value=${ex.value}"}

        val expectedType = ex.requiredType?.simpleName ?: "unknown"
        val detail = "파라미터 '${ex.name}' 의 타입 변환에 실패했습니다. 기대 타입: $expectedType"

        val pd = ErrorCode.TYPE_MISMATCH.toProblemDetail(detail, request.requestURI)
        pd.setProperty("parameterName", ex.name)
        pd.setProperty("receivedValue", ex.value.toString())

        return pd
    }

    // ✅ 필수 파라미터 누락 (MissingServletRequestParameterException)
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn{"[PARAM_MISSING] param=${ex.parameterName}"}

        val detail = "필수 파라미터 '${ex.parameterName}' 이(가) 누락되었습니다."
        val pd = ErrorCode.PARAM_MISSING.toProblemDetail(detail, request.requestURI)
        pd.setProperty("parameterName", ex.parameterName)

        return pd
    }


    // ✅ HTTP 메서드 허용 안됨 (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn { "[METHOD_NOT_ALLOWED] method=${ex.method}, supported=${ex.supportedMethods}" }

        val supported = ex.supportedMethods?.joinToString(", ") ?: "none"
        val detail = "지원하지 않는 HTTP 메서드 '${ex.method}' 입니다. 지원: $supported"

        return ErrorCode.METHOD_NOT_ALLOWED.toProblemDetail(detail, request.requestURI)
    }

    // ✅ Content-Type 지원 안됨 (415)
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(
        ex: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn{"[UNSUPPORTED_MEDIA_TYPE] contentType=${ex.contentType}"}

        return ErrorCode.UNSUPPORTED_MEDIA_TYPE.toProblemDetail(
            "지원하지 않는 Content-Type 입니다.",
            request.requestURI
        )
    }

    // ✅ DB 무결성 위반 (DataIntegrityViolationException)
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.warn{"[DB_INTEGRITY] message=${ex.mostSpecificCause.message}"}

        // ⚠️ 내부 예외 메시지 노출 금지 - 일반화된 메시지 제공
        return ErrorCode.DUPLICATE_RESOURCE.toProblemDetail(
            "이미 존재하는 리소스입니다. 또는 데이터 무결성 제약조건에 위반됩니다.",
            request.requestURI
        )
    }

    // ✅ Fallback: 모든 예상치 못한 예외 (보안 고려)
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: HttpServletRequest
    ): ProblemDetail {
        logger.error(ex) { "[INTERNAL_ERROR] unexpected exception, path=${request.requestURI}" }

        // 사용자에게는 일반화된 메시지, 로그에만 상세 스택트레이스
        val pd = ErrorCode.INTERNAL_SERVER_ERROR.toProblemDetail(
            "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.",
            request.requestURI
        )

        // ✅ traceId 지원 (있을 경우)
        request.getAttribute("traceId")?.let{
            pd.setProperty("traceId", it)
        }
        return pd
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ProblemDetail {
        return ErrorCode.INVALID_REQUEST_PARAMETER.toProblemDetail(
            "요청 본문을 읽을 수 없습니다. 날짜/시간 형식을 확인해주세요.",
            request.requestURI
        )
    }
}