package com.meisterbear.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "추천 페이지 여정 섹션")
public class JourneySectionResponse {

    @Schema(description = "섹션 제목", example = "마이스터베어와 함께하는 여정")
    private String title;

    @Schema(description = "섹션 부제", example = "나만의 참과 캐릭터를 찾아보세요")
    private String subtitle;
}
