package com.meisterbear.domain.nfc.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "NFC 태그 제품에 연결된 캐릭터 (컬렉션 추가 화면용)")
public class NfcCharacterResponse {

    @Schema(description = "캐릭터 id (컬렉션 추가 API에 이 값을 사용)", example = "1")
    private Long id;

    @Schema(description = "캐릭터 이름", example = "비세토스 라이언")
    private String name;

    @Schema(description = "소속 컬렉션명", example = "MCM BASIC COLLECTION")
    private String collectionName;

    @Schema(description = "캐릭터 소개", example = "장인 정신이 깃든 비세토스 라이언입니다.")
    private String description;

    @Schema(description = "캐릭터 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/character/1.png")
    private String imageUrl;
}
