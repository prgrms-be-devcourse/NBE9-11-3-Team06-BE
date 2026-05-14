package com.back.nbe9112team06.global.exception;

import com.back.nbe9112team06.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getStatus();
    }

    public String getErrorCode() {
        return errorCode.getCode();
    }

    @Override
    public String getMessage() {
        return super.getMessage() != null ? super.getMessage() : errorCode.getMessage();
    }

    /**
     * ProblemDetail 로 변환 (GlobalExceptionHandler 에서 사용)
     */
    public ProblemDetail toProblemDetail(String path) {
        return errorCode.toProblemDetail(this.getMessage(), path);
    }
}