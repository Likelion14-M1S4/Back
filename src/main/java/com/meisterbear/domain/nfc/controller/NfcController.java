package com.meisterbear.domain.nfc.controller;

import com.meisterbear.domain.nfc.dto.response.CertificateResponse;
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
            description = "실물 NFC(제품 부착)에 각인된 uid로 제품을 식별한다. **비로그인 호출 허용** (온보딩 퍼널: 태그→인증서→캐릭터→로그인).\n\n"
                    + "- 비로그인이면 방문 태그 이력 기록만 생략되고 검증·캐릭터 정보는 동일하게 내려간다.\n"
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
                                          "charmId": 1,
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
        // 온보딩 퍼널은 비로그인 허용(permitAll) - 익명이면 이력 기록 없이 검증만 수행
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return BaseResponse.success(nfcService.verify(userId, uid));
    }

    @Operation(
            summary = "정품 인증서 조회",
            description = "정품 인증서를 반환한다. **비로그인 호출 허용** (태그 직후 로그인 전 화면).\n\n"
                    + "- uid(NFC 태그 값)를 넘기면 **그 실물 제품의 최신 구매 기록** 기준으로 발급한다 - 유저 무관 (verify에서 받은 uid 그대로 전달).\n"
                    + "- uid를 생략하면 로그인 유저의 최근 구매 1건 기준 (비로그인+uid 없음이면 404).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정품 인증서 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "productName": "Stark 사이드 스터드 비세토스 백팩",
                                        "imageUrl": "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com/product/1.png",
                                        "orderNumber": "ORD-2026-000123",
                                        "productNumber": "MCM-BP-000001",
                                        "issuedAt": "2026.08.16",
                                        "purchasedAt": "2026.08.16 pm.03:00",
                                        "receivedAt": "2026.08.16 pm.03:30",
                                        "seller": "엠씨엠코리아",
                                        "purchasePlace": "MCM 롯데백화점 본점"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "구매 내역 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "구매 내역 없음", value = """
                                    {
                                      "success": false,
                                      "code": "NFC404",
                                      "message": "인증서를 발급할 구매 내역이 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @GetMapping("/certificate")
    public BaseResponse<CertificateResponse> getCertificate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String uid) {
        // 온보딩 퍼널은 비로그인 허용(permitAll) - uid 기반 조회는 유저 무관
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return BaseResponse.success(nfcService.getCertificate(userId, uid));
    }
}
