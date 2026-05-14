package com.back.nbe9112team06.global.exception;

import com.back.nbe9112team06.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // ✅ 비즈니스 예외 처리 (최우선)
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("[BUSINESS_ERROR] code={}, message={}, path={}",
                ex.getErrorCode(), ex.getMessage(), request.getRequestURI());

        ProblemDetail pd = ex.toProblemDetail(request.getRequestURI());

        Object traceId = request.getAttribute("traceId");
        if (traceId != null) {
            pd.setProperty("traceId", traceId.toString());
        }

        return pd;
    }

    // ✅ @Valid 검증 실패 처리 (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("[VALIDATION_ERROR] detail={}, path={}", detail, request.getRequestURI());

        ProblemDetail pd = ErrorCode.VALIDATION_FAILED.toProblemDetail(detail, request.getRequestURI());

        // ✅ validationErrors 확장 필드 추가
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("field", err.getField(), "message", err.getDefaultMessage()))
                .toList();
        pd.setProperty("validationErrors", errors);

        return pd;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidationException(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        // ✅ getParameterValidationResults() 사용 (문서 참조)
        String detail = ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    // 파라미터 이름 추출 (예: "code")
                    String paramName = result.getMethodParameter().getParameterName();

                    // 해당 파라미터의 모든 검증 에러 메시지 추출
                    return result.getResolvableErrors().stream()
                            .map(error -> {
                                String message = error.getDefaultMessage();
                                return String.format("%s: %s",
                                        paramName != null ? paramName : "unknown",
                                        message != null ? message : "유효성 검사 오류");
                            });
                })
                .collect(Collectors.joining(", "));

        log.warn("[PARAM_VALIDATION_ERROR] detail={}, path={}", detail, request.getRequestURI());

        ProblemDetail pd = ErrorCode.VALIDATION_FAILED.toProblemDetail(detail, request.getRequestURI());

        // ✅ validationErrors 확장 필드 추가 (선택사항, 디버깅용)
        List<Map<String, String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String paramName = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors().stream()
                            .map(error -> Map.of(
                                    "field", paramName != null ? paramName : "unknown",
                                    "message", error.getDefaultMessage() != null ?
                                            error.getDefaultMessage() : "유효성 검사 오류"
                            ));
                })
                .toList();
        pd.setProperty("validationErrors", errors);

        return pd;
    }

    // ✅ 404 Not Found
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("[NOT_FOUND] path={}", request.getRequestURI());

        return ErrorCode.NOT_FOUND.toProblemDetail(
                "요청한 리소스를 찾을 수 없습니다.",
                request.getRequestURI()
        );
    }

    // ✅ 타입 변환 실패 (MethodArgumentTypeMismatchException)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("[TYPE_MISMATCH] param={}, value={}", ex.getName(), ex.getValue());

        String detail = String.format("파라미터 '%s' 의 타입 변환에 실패했습니다. 기대 타입: %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ProblemDetail pd = ErrorCode.TYPE_MISMATCH.toProblemDetail(detail, request.getRequestURI());
        pd.setProperty("parameterName", ex.getName());
        pd.setProperty("receivedValue", String.valueOf(ex.getValue()));

        return pd;
    }

    // ✅ 필수 파라미터 누락 (MissingServletRequestParameterException)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("[PARAM_MISSING] param={}", ex.getParameterName());

        String detail = String.format("필수 파라미터 '%s' 이(가) 누락되었습니다.", ex.getParameterName());
        ProblemDetail pd = ErrorCode.PARAM_MISSING.toProblemDetail(detail, request.getRequestURI());
        pd.setProperty("parameterName", ex.getParameterName());

        return pd;
    }


    // ✅ HTTP 메서드 허용 안됨 (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("[METHOD_NOT_ALLOWED] method={}, supported={}",
                ex.getMethod(), ex.getSupportedMethods());

        String detail = String.format("지원하지 않는 HTTP 메서드 '%s' 입니다. 지원: %s",
                ex.getMethod(), ex.getSupportedMethods() != null ?
                        String.join(", ", ex.getSupportedMethods()) : "none");

        return ErrorCode.METHOD_NOT_ALLOWED.toProblemDetail(detail, request.getRequestURI());
    }

    // ✅ Content-Type 지원 안됨 (415)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("[UNSUPPORTED_MEDIA_TYPE] contentType={}", ex.getContentType());

        return ErrorCode.UNSUPPORTED_MEDIA_TYPE.toProblemDetail(
                "지원하지 않는 Content-Type 입니다.",
                request.getRequestURI()
        );
    }

    // ✅ DB 무결성 위반 (DataIntegrityViolationException)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("[DB_INTEGRITY] message={}", ex.getMostSpecificCause().getMessage());

        // ⚠️ 내부 예외 메시지 노출 금지 - 일반화된 메시지 제공
        return ErrorCode.DUPLICATE_RESOURCE.toProblemDetail(
                "이미 존재하는 리소스입니다. 또는 데이터 무결성 제약조건에 위반됩니다.",
                request.getRequestURI()
        );
    }

    // ✅ Fallback: 모든 예상치 못한 예외 (보안 고려)
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(
            Exception ex, HttpServletRequest request) {

        log.error("[INTERNAL_ERROR] unexpected exception, path={}",
                request.getRequestURI(), ex);

        // 사용자에게는 일반화된 메시지, 로그에만 상세 스택트레이스
        ProblemDetail pd = ErrorCode.INTERNAL_SERVER_ERROR.toProblemDetail(
                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.",
                request.getRequestURI()
        );

        // ✅ traceId 지원 (있을 경우)
        Object traceId = request.getAttribute("traceId");
        if (traceId != null) {
            pd.setProperty("traceId", traceId.toString());
        }

        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return ErrorCode.INVALID_REQUEST_PARAMETER.toProblemDetail(
                "요청 본문을 읽을 수 없습니다. 날짜/시간 형식을 확인해주세요.",
                request.getRequestURI()
        );
    }
}