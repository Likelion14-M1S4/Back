package com.meisterbear.domain.chat.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    CHARACTER_NOT_FOUND("CHAT404", "해당 캐릭터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHARACTER_NOT_OWNED("CHAT404", "아직 만나지 않은 캐릭터입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
