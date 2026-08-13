package com.meisterbear.domain.wishlist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "위시리스트 토글 요청. productId, charmId 중 정확히 하나만 보낸다(둘 다 보내거나 둘 다 비우면 400)",
        example = "{\"productId\":1,\"charmId\":null}")
public class ToggleWishlistRequest {

    @Schema(description = "찜/해제할 product.id. charmId와 동시에 보내지 않는다", example = "1")
    private Long productId;

    @Schema(description = "찜/해제할 charm.id. productId와 동시에 보내지 않는다")
    private Long charmId;
}
