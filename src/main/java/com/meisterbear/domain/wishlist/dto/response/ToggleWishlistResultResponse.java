package com.meisterbear.domain.wishlist.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meisterbear.domain.wishlist.entity.WishlistItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "위시리스트 토글 결과")
public class ToggleWishlistResultResponse {

    @Schema(description = "토글한 대상 종류", example = "PRODUCT")
    private WishlistItemType type;

    @Schema(description = "토글한 product.id 또는 charm.id", example = "5")
    private Long targetId;

    @JsonProperty("isWished")
    @Schema(description = "토글 처리 후 최종 찜 상태. true면 방금 찜 추가된 것(하트 채움), "
            + "false면 방금 찜 해제된 것(하트 비움 / 목록에서 제거)", example = "true")
    private boolean wished;
}
