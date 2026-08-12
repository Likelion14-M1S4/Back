package com.meisterbear.domain.story.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StoryErrorCode implements BaseErrorCode {

    STORY_NOT_FOUND("STORY404", "해당 스토리를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STORY_LOCKED("STORY403", "잠긴 챕터입니다.", HttpStatus.FORBIDDEN),
    INVALID_CHOICE("STORY400", "유효하지 않은 선택지입니다.", HttpStatus.BAD_REQUEST),
    ALREADY_COMPLETED("STORY409", "이미 완주한 챕터입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
