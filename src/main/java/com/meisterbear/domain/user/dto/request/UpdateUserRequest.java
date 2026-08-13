package com.meisterbear.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "내 정보 수정 요청")
public class UpdateUserRequest {

    @Schema(description = "선택 필드. 안 보내거나 null이면 기존 닉네임 유지", example = "박세은")
    private String nickname;

    @Schema(description = "선택 필드. 안 보내거나 null이면 기존 전화번호 유지. "
            + "구분자 상관없이 숫자만 뽑아 10~11자리면 통과(하이픈/공백/구분자 없음 다 허용), 아니면 400", example = "010 9973 7761")
    private String phone;

    @Schema(description = "선택 필드. 안 보내거나 null이면 기존 생년월일 유지. "
            + "YYYY-MM-DD 형식 고정(구분자는 반드시 하이픈). 월/일을 모르면 00으로 보내도 됨", example = "2000-00-00")
    private String birthDate;
}
