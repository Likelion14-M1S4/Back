package com.meisterbear.domain.auth.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_KAKAO_TOKEN("AUTH401", "유효하지 않은 카카오 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("AUTH401", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    KAKAO_SERVER_ERROR("AUTH502", "카카오 서버 통신에 실패했습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
