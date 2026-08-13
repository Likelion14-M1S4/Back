package com.meisterbear.domain.wishlist.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WishlistErrorCode implements BaseErrorCode {

    INVALID_TARGET("WISHLIST400", "productId와 charmId 중 정확히 하나만 지정해야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
