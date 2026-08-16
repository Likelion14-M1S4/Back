package com.meisterbear.domain.nfc.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "정품 인증서 응답")
public class CertificateResponse {

    @Schema(description = "제품명", example = "Stark 사이드 스터드 비세토스 백팩")
    private String productName;

    @Schema(description = "제품 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png")
    private String imageUrl;

    @Schema(description = "주문 번호", example = "ORD-2026-000123")
    private String orderNumber;

    @Schema(description = "제품 번호 (시리얼)", example = "MCM-BP-000001")
    private String productNumber;

    @Schema(description = "인증서 발급일", example = "2026.08.16")
    private String issuedAt;

    @Schema(description = "구매일시 (화면 표기 포맷)", example = "2026.08.16 pm.03:00")
    private String purchasedAt;

    @Schema(description = "수령일시 (화면 표기 포맷)", example = "2026.08.16 pm.03:30")
    private String receivedAt;

    @Schema(description = "판매자", example = "엠씨엠코리아")
    private String seller;

    @Schema(description = "구매처(매장명)", example = "MCM 롯데백화점 본점")
    private String purchasePlace;
}
