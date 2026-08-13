package com.meisterbear.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내 정보 조회 응답")
public class UserResponse {

    @Schema(description = "user.email", example = "seeunbag33@skuniv.ac.kr")
    private String email;

    @Schema(description = "user.nickname", example = "박세은")
    private String nickname;

    @Schema(description = "user.phone", example = "010 9973 7761")
    private String phone;

    @Schema(description = "user.birth_date", example = "2000-00-00")
    private String birthDate;
}
