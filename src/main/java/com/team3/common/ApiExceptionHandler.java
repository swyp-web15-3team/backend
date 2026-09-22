package com.team3.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.team3.common.exception.CustomException;
import com.team3.common.exception.ErrorCode;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed.");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toValidationError)
            .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        TypeMismatchException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request) {
        if ("whiskyId".equals(ex.getPropertyName())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "위스키 ID가 올바르지 않습니다.");
            return handleExceptionInternal(ex, problem, headers, status, request);
        }
        if ("plannerItemId".equals(ex.getPropertyName())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "플래너 항목 ID가 올바르지 않습니다.");
            return handleExceptionInternal(ex, problem, headers, status, request);
        }
        if ("saleProductId".equals(ex.getPropertyName())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "판매 상품 ID가 올바르지 않습니다.");
            return handleExceptionInternal(ex, problem, headers, status, request);
        }
        return super.handleTypeMismatch(ex, headers, status, request);
    }

    private Map<String, String> toValidationError(FieldError error) {
        String message = Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value.");
        return Map.of("field", error.getField(), "message", message);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
        problem.setProperty("code", errorCode.getCode());
        ex.getProperties().forEach(problem::setProperty);
        return handleExceptionInternal(ex, problem, new HttpHeaders(), errorCode.getStatus(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        LOG.error("Unexpected request failure", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        return handleExceptionInternal(
            ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
