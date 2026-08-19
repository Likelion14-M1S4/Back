package com.meisterbear.domain.character.controller;

import com.meisterbear.domain.character.dto.response.CollectCharacterResponse;
import com.meisterbear.domain.character.service.CharacterService;
import com.meisterbear.global.common.BaseResponse;
import com.meisterbear.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Character", description = "캐릭터 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
public class CharacterController {


    private final CharacterService characterService;

    @Operation(
            summary = "캐릭터 컬렉션 추가",
            description = "NFC 태그로 얻은 캐릭터를 내 컬렉션에 추가한다(OWNED). "
                    + "characterId는 NFC 태그 검증(GET /api/nfc/verify) 응답의 character.id를 사용한다. "
                    + "이미 수집한 캐릭터를 다시 추가해도 에러 없이 성공으로 응답한다(멱등).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "컬렉션 추가 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "추가 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "캐릭터가 컬렉션에 추가되었습니다.",
                                      "data": { "id": 1, "collected": true }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "캐릭터 없음", value = """
                                    {
                                      "success": false,
                                      "code": "CHARACTER404",
                                      "message": "해당 캐릭터를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/{characterId}/collect")
    public BaseResponse<CollectCharacterResponse> collect(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long characterId) {
        CollectCharacterResponse response =
                characterService.collect(userDetails.getUser().getId(), characterId);
        return BaseResponse.success("캐릭터가 컬렉션에 추가되었습니다.", response);
    }
}
