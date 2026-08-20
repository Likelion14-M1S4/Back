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
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Chat", description = "동행 챗봇 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    // OpenAI 응답 지연을 감안해 넉넉히 잡음
    private static final long STREAM_TIMEOUT_MS = 60_000L;

    private final ChatService chatService;

    @Operation(
            summary = "채팅 진입 화면 조회",
            description = "제품 상세의 '캐릭터와 대화' 버튼으로 채팅 화면에 진입할 때 호출한다. 동행 정보, 유저 닉네임이 반영된 인사말, "
                    + "대화 시작 선택지 2개를 반환한다. 대화 기록은 저장하지 않으므로 이 API는 매번 같은 초기 상태를 반환하며, "
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
                                        "charmId": 1,
                                        "characterName": "비세토스 라이언",
                                        "characterImgUrl": "https://cdn.meisterbear.com/character/1.png",
                                        "greeting": "안녕하세요, 박세은님. 어떤 얘기를 나눠볼까요?",
                                        "starterChoices": [
                                          { "id": 1, "label": "제품이 오염됐어", "tagName": "care" },
                                          { "id": 2, "label": "너에 대해 알고싶어", "tagName": "character" }
                                        ]
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터",
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
    @GetMapping("/{characterId}/entry")
    public BaseResponse<ChatEntryResponse> findEntry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long characterId) {
        Long userId = userDetails.getUser().getId();
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
                                        "charmId": 1,
                                        "reply": "이번 시즌 비세토스 라이언 백팩은 최상급 새들 레더로 만들어졌어요. 궁금한 게 더 있나요?"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터",
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
            @Valid @RequestBody SendChatMessageRequest request) {
        Long userId = userDetails.getUser().getId();
        ChatMessageResultResponse response = chatService.sendMessage(userId, request);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "챗봇 메세지 전송(스트리밍)",
            description = "위 /messages와 캐릭터 조회·프롬프트·대체 문구 로직은 완전히 동일하고, 응답 방식만 다르다. "
                    + "완성된 답변을 한 번에 안 주고 AI가 토큰을 만드는 대로 SSE(text/event-stream)로 흘려보내서 체감 응답 속도를 "
                    + "높인다. 각 이벤트의 data는 순수 텍스트 조각(delta)이며, 프론트는 도착하는 대로 이어붙여서 렌더링한다. "
                    + "스트림이 끝나면(정상 완료든 실패 후 대체 문구든) 연결이 닫힌다 - 별도의 종료 신호 이벤트는 없다. "
                    + "이 API는 BaseResponse로 감싸지 않는다(SSE는 JSON 단일 응답이 아니라서 컨벤션 예외).\n\n"
                    + "history 형식은 /messages와 동일하다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "text/event-stream으로 토큰이 순서대로 도착",
                    content = @Content(mediaType = "text/event-stream",
                            examples = @ExampleObject(name = "스트리밍 예시", value = """
                                    data:이번

                                    data: 시즌

                                    data: 비세토스

                                    data: 라이언

                                    data: 백팩은...

                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터",
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
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SendChatMessageRequest request) {
        Long userId = userDetails.getUser().getId();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        chatService.sendMessageStream(userId, request, emitter);
        return emitter;
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
                                        "charmId": 1,
                                        "reply": "어떤 이유로 생긴 얼룩인가요?"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터",
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
        Long userId = userDetails.getUser().getId();
        ChatMessageResultResponse response = chatService.inspect(userId, characterId, history, image);
        return BaseResponse.success(response);
    }

    @Operation(
            summary = "AI 인스펙터 - 사진으로 케어 진단(스트리밍)",
            description = "위 /inspector와 로직은 완전히 동일하고, 응답만 완성된 답변을 한 번에 안 주고 SSE(text/event-stream)로 "
                    + "토큰 단위로 흘려보낸다. 실패 시 대체 문구도 스트림으로 한 번에 전송된다. "
                    + "이 API는 BaseResponse로 감싸지 않는다(SSE는 JSON 단일 응답이 아니라서 컨벤션 예외).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "text/event-stream으로 토큰이 순서대로 도착"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 캐릭터",
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
    @PostMapping(value = "/inspector/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter inspectStream(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long characterId,
            @RequestParam(required = false) String history,
            @RequestPart MultipartFile image) {
        Long userId = userDetails.getUser().getId();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        chatService.inspectStream(userId, characterId, history, image, emitter);
        return emitter;
    }
}
