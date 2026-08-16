package com.meisterbear.domain.nfc.controller;

import com.meisterbear.domain.nfc.dto.response.NfcVerifyResponse;
import com.meisterbear.domain.nfc.service.NfcService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "NFC", description = "NFC 태그 / 정품 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nfc")
public class NfcController {

    private final NfcService nfcService;

    @Operation(
            summary = "NFC 태그 검증",
            description = "실물 NFC(제품 부착)에 각인된 uid로 제품을 식별한다.\n\n"
                    + "- 응답의 character를 캐릭터 컬렉션 추가 화면에 사용한다 (없으면 null).\n"
                    + "- 부수효과: 제품의 진열 매장으로 방문 태그 이력이 기록된다 (매장 태그 이력 화면에 반영).\n"
                    + "- 검증 후 uid를 그대로 정품 인증서 조회(GET /api/nfc/certificate?uid=)에 넘긴다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "NFC 태그 검증 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "검증 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "type": "PRODUCT",
                                        "productId": 1,
                                        "productName": "Stark 사이드 스터드 비세토스 백팩",
                                        "character": {
                                          "id": 1,
                                          "name": "비세토스 라이언",
                                          "collectionName": "MCM BASIC COLLECTION",
                                          "description": "장인 정신이 깃든 비세토스 라이언입니다.",
                                          "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/character/1.png"
                                        },
                                        "nextPath": "/store-tag/certificate"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "미등록 NFC",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "미등록 NFC", value = """
                                    {
                                      "success": false,
                                      "code": "NFC404",
                                      "message": "등록되지 않은 NFC 태그입니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/verify")
    public BaseResponse<NfcVerifyResponse> verify(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String uid) {
        return BaseResponse.success(nfcService.verify(userDetails.getUser().getId(), uid));
    }
}
