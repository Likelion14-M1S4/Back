package com.meisterbear.domain.wishlist.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "위시리스트 목록 조회 응답")
public class WishlistListResponse {

    @Schema(description = "찜한 순서(최근 찜한 순)로 정렬된 목록",
            example = "[{\"id\":1,\"type\":\"PRODUCT\",\"targetId\":1,\"name\":\"Stark 사이드 스터드 비세토스\","
                    + "\"imgUrl\":\"https://cdn.meisterbear.com/product/1.png\",\"price\":1490000}]")
    private List<WishlistItemResponse> items;

    public static WishlistListResponse empty() {
        return WishlistListResponse.builder()
                .items(List.of())
                .build();
    }
}
