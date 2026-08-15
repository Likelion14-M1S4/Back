package com.meisterbear.domain.character.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CharacterErrorCode implements BaseErrorCode {

    CHARACTER_NOT_FOUND("CHARACTER404", "해당 캐릭터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
