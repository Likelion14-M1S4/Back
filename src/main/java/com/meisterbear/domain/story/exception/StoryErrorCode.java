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
    INVALID_CHOICE("STORY400", "존재하지 않는 선택지입니다.", HttpStatus.BAD_REQUEST),
    CHARACTER_NOT_FOUND("STORY500", "컬렉션이 가리키는 캐릭터를 찾을 수 없습니다. 데이터 정합성을 확인하세요.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    PRODUCT_NOT_FOUND("STORY500", "캐릭터가 가리키는 제품을 찾을 수 없습니다. 데이터 정합성을 확인하세요.",
            HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
