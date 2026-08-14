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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
            summary = "챗봇 메세지 전송",
            description = "동행과 자유롭게 대화한다. AI 호출이 실패해도 에러 없이 고정 대체 문구로 대체해 항상 200을 반환한다.\n\n"
                    + "history: 지금까지 나눈 대화(유저 발화 + 캐릭터 답변)를 "
                    + "[{\"role\":\"USER\",\"content\":\"...\"},{\"role\":\"CHARACTER\",\"content\":\"...\"}] 배열로 담아서 보낸다. "
                    + "서버는 대화를 저장하지 않으므로 안 보내면 AI가 맥락을 전혀 모른다.")
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

    @Operation(
            summary = "AI 인스펙터 - 사진으로 케어 진단",
            description = "케어 문의 중 사진을 업로드하면, AI가 사진을 보고 관찰 내용·권장 케어·매장 안내를 대화 형식(reply)으로 반환한다. "
                    + "사진은 저장하지 않고 분석 즉시 버리며, 실패해도 소재 기준 케어 가이드로 대체해 항상 200을 반환한다.\n\n"
                    + "history: 지금까지 나눈 대화(유저 발화 + 캐릭터 답변)를 "
                    + "'[{\"role\":\"USER\",\"content\":\"...\"},{\"role\":\"CHARACTER\",\"content\":\"...\"}]' 형태의 "
                    + "JSON 문자열로 담아서 보낸다. 서버는 대화를 저장하지 않으므로 안 보내면 AI가 맥락을 전혀 모른다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "케어 진단 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "진단 성공", value = """
                                    {
                                      "success": true,
                                      "code": 200,
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "characterId": 1,
                                        "reply": "어떤 이유로 생긴 얼룩인가요?"
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
                                    """))),
            @ApiResponse(responseCode = "413", description = "업로드 파일 용량 초과 (최대 10MB)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BaseResponse.class),
                            examples = @ExampleObject(name = "용량 초과", value = """
                                    {
                                      "success": false,
                                      "code": "G007",
                                      "message": "업로드 파일 용량이 너무 큽니다. (최대 10MB)",
                                      "data": null
                                    }
                                    """)))
    })
    @PostMapping(value = "/inspector", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<ChatMessageResultResponse> inspect(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long characterId,
            @RequestParam(required = false) String history,
            @RequestPart MultipartFile image) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : TEMP_TEST_USER_ID;
        ChatMessageResultResponse response = chatService.inspect(userId, characterId, history, image);
        return BaseResponse.success(response);
    }
}
