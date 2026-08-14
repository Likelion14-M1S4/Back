package com.meisterbear.domain.chat.controller;

import com.meisterbear.domain.chat.dto.request.SendChatMessageRequest;
import com.meisterbear.domain.chat.dto.response.ChatEntryResponse;
import com.meisterbear.domain.chat.dto.response.ChatMessageResultResponse;
import com.meisterbear.domain.chat.service.ChatService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "동행 챗봇 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    // TODO: 로그인(JWT 발급) 구현되면 이 상수와 SecurityConfig의 permitAll("/api/chat/**") 함께 제거
    private static final Long TEMP_TEST_USER_ID = 1L;

    private final ChatService chatService;

    @Operation(
            summary = "채팅 진입 화면 조회",
            description = "제품 상세의 '캐릭터와 대화' 버튼으로 채팅 화면에 진입할 때 호출한다. 동행 정보, 유저 닉네임이 반영된 인사말, "
                    + "대화 시작 선택지 3개를 반환한다. 대화 기록은 저장하지 않으므로 이 API는 매번 같은 초기 상태를 반환하며, "
                    + "이후 대화는 POST /api/chat/messages로 이어진다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅 진입 화면 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "조회 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "characterId": 1,
                                        "characterName": "비세토스 라이언",
                                        "characterImgUrl": "https://cdn.meisterbear.com/character/1.png",
                                        "greeting": "안녕하세요, 박세은님. 어떤 얘기를 나눠볼까요?",
                                        "starterChoices": [
                                          { "id": 1, "label": "제품이 오염됐어", "tagName": "care" },
                                          { "id": 2, "label": "이 제품에 대해 알려줘", "tagName": "product" },
                                          { "id": 3, "label": "너에 대해 알고싶어", "tagName": "character" }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터, 또는 이 유저가 아직 만나지 않은 캐릭터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = {
                                    @ExampleObject(name = "캐릭터 없음", value = """
                                            {
                                              "success": false,
                                              "code": "CHAT404",
                                              "message": "해당 캐릭터를 찾을 수 없습니다.",
                                              "data": null
                                            }
                                            """),
                                    @ExampleObject(name = "아직 만나지 않은 캐릭터", value = """
                                            {
                                              "success": false,
                                              "code": "CHAT404",
                                              "message": "아직 만나지 않은 캐릭터입니다.",
                                              "data": null
                                            }
                                            """)
                            }))
    })
    @GetMapping("/{characterId}/entry")
    public BaseResponse<ChatEntryResponse> findEntry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long characterId) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        ChatEntryResponse response = chatService.findEntry(userId, characterId);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "동행과 대화",
            description = "동행과 자유롭게 대화한다. 대화 기록은 저장하지 않으므로, 맥락이 필요하면 클라이언트가 이전 대화 "
                    + "내역(history)을 매 요청마다 같이 실어 보낸다. "
                    + "GPT API 호출 자체가 실패 시, reply에 고정 대체 문구가 담겨 200으로 응답.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "대화 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "characterId": 1,
                                        "reply": "이번 시즌 비세토스 라이언 백팩은 최상급 새들 레더로 만들어졌어요. 궁금한 게 더 있나요?"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터, 또는 이 유저가 아직 만나지 않은 캐릭터",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "캐릭터 없음", value = """
                                    {
                                      "success": false,
                                      "code": "CHAT404",
                                      "message": "해당 캐릭터를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping("/messages")
    public BaseResponse<ChatMessageResultResponse> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SendChatMessageRequest request) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        ChatMessageResultResponse response = chatService.sendMessage(userId, request);
        return BaseResponse.success(response);
    }
}
