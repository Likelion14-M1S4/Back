package com.meisterbear.domain.wishlist.dto.response;

import com.meisterbear.domain.wishlist.entity.WishlistItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "찜한 항목")
public class WishlistItemResponse {

    @Schema(description = "wishlist.id (찜 해제 시 필요하면 사용)", example = "1")
    private Long id;

    @Schema(description = "찜한 대상 종류. PRODUCT면 targetId=product.id, CHARM이면 targetId=charm.id",
            example = "PRODUCT")
    private WishlistItemType type;

    @Schema(description = "대상의 product.id 또는 charm.id (type으로 구분)", example = "1")
    private Long targetId;

    @Schema(description = "product.name 또는 charm.name", example = "Stark 사이드 스터드 비세토스")
    private String name;

    @Schema(description = "product.img_url 또는 charm.img_url", example = "https://cdn.meisterbear.com/product/1.png")
    private String imgUrl;

    @Schema(description = "product.price 또는 charm.price", example = "1490000")
    private Integer price;
}
