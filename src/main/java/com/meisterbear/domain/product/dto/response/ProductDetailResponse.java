package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "제품 상세 조회 응답")
public class ProductDetailResponse {

    @Schema(description = "제품 id", example = "1")
    private Long id;

    @Schema(description = "제품명", example = "Stark 사이드 스터드 비세토스 백팩")
    private String name;

    @Schema(description = "가격(원)", example = "1490000")
    private Integer price;

    @Schema(description = "대표 이미지 URL",
            example = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png")
    private String imageUrl;

    @Schema(description = "로그인 유저의 찜 여부", example = "false")
    private Boolean isLiked;

    @Schema(description = "현재 제품의 색상명", example = "Soft Pink")
    private String colorLabel;

    @Schema(description = "선택 가능한 색상 옵션 (색상 형제 제품들, 자기 자신 포함)")
    private List<ProductColorResponse> colors;

    @Schema(description = "선택 가능한 사이즈 목록", example = "[\"미니\", \"S\"]")
    private List<String> sizes;

    @Schema(description = "현재 제품의 사이즈", example = "미니")
    private String selectedSize;

    @Schema(description = "매장 확인 버튼 문구", example = "구매 가능 매장 확인하기")
    private String storeCheckLabel;

    @Schema(description = "매장 확인 이동 경로", example = "/story/stores")
    private String storeUrl;

    @Schema(description = "상세 설명 블록")
    private ProductDetailSectionResponse detail;

    @Schema(description = "로그인 유저의 구매 여부 (시즌 제품 상세용)", example = "false")
    private Boolean isPurchased;

    @Schema(description = "스토리 완주가 필요한 시즌 한정 제품 여부", example = "false")
    private Boolean requiresStory;
}
