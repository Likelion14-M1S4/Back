package com.meisterbear.domain.user.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND("USER404", "해당 유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_PHONE_FORMAT("USER400", "전화번호는 숫자 10~11자리여야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_BIRTH_DATE_FORMAT("USER400", "생년월일 형식이 올바르지 않습니다. (예: 2000-00-00)", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
