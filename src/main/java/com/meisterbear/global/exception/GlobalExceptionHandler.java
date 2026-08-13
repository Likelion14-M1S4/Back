package com.meisterbear.global.exception;

import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.global.exception.model.BaseErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 도메인 커스텀 예외 - {Domain}ErrorCode 기반
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.warn("[GlobalExceptionHandler] CustomException 발생 - code={}, message={}",
                errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // @Valid 검증 실패 (Request Body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> String.format("[%s] %s", e.getField(), e.getDefaultMessage()))
                .collect(Collectors.joining(" / "));
        log.warn("[GlobalExceptionHandler] Validation 실패 - {}", message);
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    // PathVariable/RequestParam 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Object>> handleConstraintViolation(
            ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> String.format("[%s] %s", v.getPropertyPath(), v.getMessage()))
                .collect(Collectors.joining(" / "));
        log.warn("[GlobalExceptionHandler] 제약 조건 위반 - {}", message);
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), message));
    }

    // Request Body 형식 오류 (JSON 파싱 실패)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {
        log.warn("[GlobalExceptionHandler] JSON 파싱 오류 - {}", ex.getMessage());
        return ResponseEntity.status(GlobalErrorCode.INVALID_JSON_FORMAT.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INVALID_JSON_FORMAT.getCode(),
                        GlobalErrorCode.INVALID_JSON_FORMAT.getMessage()));
    }

    // 예상치 못한 서버 오류 (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleException(Exception ex) {
        log.error("[GlobalExceptionHandler] 예상치 못한 서버 오류", ex);
        return ResponseEntity.status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
