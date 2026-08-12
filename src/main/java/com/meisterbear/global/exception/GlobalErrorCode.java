package com.meisterbear.global.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

    INVALID_INPUT_VALUE("G001", "유효하지 않은 입력입니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("G002", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("G003", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    METHOD_NOT_ALLOWED("G004", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_JSON_FORMAT("G005", "요청 본문의 JSON 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("G006", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
