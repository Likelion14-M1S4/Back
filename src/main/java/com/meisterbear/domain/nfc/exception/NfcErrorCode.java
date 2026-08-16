package com.meisterbear.domain.nfc.exception;

import com.meisterbear.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NfcErrorCode implements BaseErrorCode {

    NFC_NOT_FOUND("NFC404", "등록되지 않은 NFC 태그입니다.", HttpStatus.NOT_FOUND),
    CERTIFICATE_NOT_FOUND("NFC404", "인증서를 발급할 구매 내역이 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
