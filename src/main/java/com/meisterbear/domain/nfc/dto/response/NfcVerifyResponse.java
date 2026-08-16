package com.meisterbear.domain.nfc.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "NFC 태그 검증 응답")
public class NfcVerifyResponse {

    @Schema(description = "태그 대상 타입 (현재 제품 태그만 지원)", example = "PRODUCT")
    private String type;

    @Schema(description = "태그한 제품 id", example = "1")
    private Long productId;

    @Schema(description = "태그한 제품명", example = "Stark 사이드 스터드 비세토스 백팩")
    private String productName;

    @Schema(description = "제품에 연결된 캐릭터 (없으면 null)")
    private NfcCharacterResponse character;

    @Schema(description = "검증 후 이동 경로", example = "/store-tag/certificate")
    private String nextPath;
}
