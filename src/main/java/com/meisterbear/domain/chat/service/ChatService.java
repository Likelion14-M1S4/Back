package com.meisterbear.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meisterbear.domain.chat.client.ChatTurn;
import com.meisterbear.domain.chat.client.OpenAiClient;
import com.meisterbear.domain.chat.dto.request.ChatHistoryMessage;
import com.meisterbear.domain.chat.dto.request.SendChatMessageRequest;
import com.meisterbear.domain.chat.dto.response.ChatEntryResponse;
import com.meisterbear.domain.chat.dto.response.ChatMessageResultResponse;
import com.meisterbear.domain.chat.dto.response.StarterChoiceResponse;
import com.meisterbear.domain.chat.exception.ChatErrorCode;
import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.user.entity.User;
import com.meisterbear.domain.user.exception.UserErrorCode;
import com.meisterbear.domain.user.repository.UserRepository;
import com.meisterbear.global.exception.CustomException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 클래스 레벨 @Transactional을 두지 않는다 - sendMessage/inspect는 OpenAI 호출(수 초까지 걸릴 수 있음)을 포함하는데,
// 트랜잭션으로 감싸면 그 시간 내내 DB 커넥션을 붙들고 있게 된다. 캐릭터 조회 같은 개별 DB 조회는
// Spring Data JPA가 리포지토리 메서드 단위로 자체 트랜잭션을 열어주므로 별도 래핑 없이도 동작한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // 진입 화면에 항상 고정으로 노출되는 대화 시작 선택지 - care를 고르면 프론트가 이후 사진 업로드를 /inspector로 보냄
    private static final List<StarterChoiceResponse> STARTER_CHOICES = List.of(
            StarterChoiceResponse.builder().id(1L).label("제품이 오염됐어").tagName("care").build(),
            StarterChoiceResponse.builder().id(2L).label("너에 대해 알고싶어").tagName("character").build()
    );

    // 스트리밍은 SseEmitter를 컨트롤러에 즉시 반환해야 해서, OpenAI 호출은 별도 스레드에서 돌린다.
    // 가상 스레드라 블로킹 IO(스트리밍 읽기)를 오래 잡고 있어도 플랫폼 스레드를 점유하지 않는다.
    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ChatEntryResponse findEntry(Long userId, Long characterId) {
        Character character = resolveCharacter(characterId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        log.info("[ChatService] 채팅 진입 화면 조회 완료 - userId={}, characterId={}", userId, characterId);
        return ChatEntryResponse.builder()
                .characterId(character.getId())
                .characterName(character.getName())
                .characterImgUrl(character.getImgUrl())
                .greeting("안녕하세요, " + user.getNickname() + "님. 어떤 얘기를 나눠볼까요?")
                .starterChoices(STARTER_CHOICES)
                .build();
    }

    // AI 호출이 실패하거나 지연돼도 오류를 던지지 않고 캐릭터 톤 대체 문구로 항상 200을 반환한다
    public ChatMessageResultResponse sendMessage(Long userId, SendChatMessageRequest request) {
        Character character = resolveCharacter(request.getCharacterId());

        String reply;
        try {
            String systemPrompt = ChatPromptTemplate.systemPrompt(character);
            List<ChatTurn> history = toChatTurns(request.getHistory());
            reply = openAiClient.chat(systemPrompt, history, request.getMessage());
        } catch (Exception e) {
            log.warn("[ChatService] AI 응답 생성 실패 - userId={}, characterId={}, error={}",
                    userId, request.getCharacterId(), e.getMessage());
            reply = ChatPromptTemplate.fallbackReply();
        }

        log.info("[ChatService] 대화 처리 완료 - userId={}, characterId={}", userId, request.getCharacterId());
        return ChatMessageResultResponse.builder()
                .characterId(character.getId())
                .reply(reply)
                .build();
    }

    // sendMessage와 캐릭터 조회·프롬프트·히스토리 변환·폴백 로직은 동일하고, 응답만 한 번에 안 주고
    // 토큰이 만들어지는 대로 SSE로 흘려보낸다. 캐릭터가 없으면(404) 이 메서드 안에서 즉시 던져서
    // 컨트롤러가 SseEmitter를 만들기 전에 평소처럼 GlobalExceptionHandler가 처리하게 한다.
    public void sendMessageStream(Long userId, SendChatMessageRequest request, SseEmitter emitter) {
        Character character = resolveCharacter(request.getCharacterId());
        String systemPrompt = ChatPromptTemplate.systemPrompt(character);
        List<ChatTurn> history = toChatTurns(request.getHistory());

        STREAM_EXECUTOR.execute(() -> {
            try {
                openAiClient.chatStream(systemPrompt, history, request.getMessage(),
                        token -> sendChunk(emitter, token));
                emitter.complete();
                log.info("[ChatService] 스트리밍 대화 처리 완료 - userId={}, characterId={}",
                        userId, request.getCharacterId());
            } catch (Exception e) {
                log.warn("[ChatService] 스트리밍 AI 응답 생성 실패 - userId={}, characterId={}, error={}",
                        userId, request.getCharacterId(), e.getMessage());
                sendChunk(emitter, ChatPromptTemplate.fallbackReply());
                emitter.complete();
            }
        });
    }

    // 이미 완료/타임아웃된 emitter에 보내면 예외가 나므로, 개별 청크 전송 실패로 스트림 전체가 끊기지 않게 여기서 흡수한다
    private void sendChunk(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (IOException e) {
            log.warn("[ChatService] SSE 청크 전송 실패 - error={}", e.getMessage());
        }
    }

    // 케어 문의 - 사진을 분석해서 관찰 내용/권장 케어를 대화체로 돌려준다. 텍스트는 안 받고 사진만 받는다
    // (직전까지 나눈 대화 맥락은 history로 이어받는다 - 대화 기록 미저장 정책이라 프론트가 매번 실어 보내야 함).
    // history는 multipart 파트라 JSON 문자열로 받는다 - @RequestPart로 객체를 바로 받으면 Swagger 등 일부
    // 클라이언트가 Content-Type을 application/json으로 안 붙여줘서 415가 나는 문제가 있었음.
    // 분석이 실패해도 오류를 던지지 않고, 캐릭터가 속한 제품의 소재 기준 고정 케어 가이드로 대체해 항상 200을 반환한다.
    public ChatMessageResultResponse inspect(Long userId, Long characterId, String historyJson,
                                              MultipartFile image) {
        Character character = resolveCharacter(characterId);

        String reply;
        try {
            String systemPrompt = ChatPromptTemplate.inspectorSystemPrompt(character);
            reply = openAiClient.chatWithImage(systemPrompt, toChatTurns(parseHistory(historyJson)),
                    image.getBytes(), image.getContentType());
        } catch (IOException | RuntimeException e) {
            log.warn("[ChatService] 케어 진단 실패 - userId={}, characterId={}, error={}",
                    userId, characterId, e.getMessage());
            reply = careGuideFallback(character);
        }

        log.info("[ChatService] 케어 진단 처리 완료 - userId={}, characterId={}", userId, characterId);
        return ChatMessageResultResponse.builder()
                .characterId(character.getId())
                .reply(reply)
                .build();
    }

    // inspect의 스트리밍 버전. MultipartFile의 바이트는 비동기 스레드로 넘어가기 전에(요청이 끝나기 전에)
    // 미리 읽어둔다 - 스레드 넘어간 뒤에 읽으면 요청 종료와 함께 임시 파일이 정리돼 있을 수 있어서다.
    public void inspectStream(Long userId, Long characterId, String historyJson, MultipartFile image,
                               SseEmitter emitter) {
        Character character = resolveCharacter(characterId);
        String systemPrompt = ChatPromptTemplate.inspectorSystemPrompt(character);
        List<ChatTurn> history = toChatTurns(parseHistory(historyJson));
        String contentType = image.getContentType();

        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            log.warn("[ChatService] 스트리밍 케어 진단 이미지 읽기 실패 - userId={}, characterId={}, error={}",
                    userId, characterId, e.getMessage());
            sendChunk(emitter, careGuideFallback(character));
            emitter.complete();
            return;
        }

        STREAM_EXECUTOR.execute(() -> {
            try {
                openAiClient.chatWithImageStream(systemPrompt, history, imageBytes, contentType,
                        token -> sendChunk(emitter, token));
                emitter.complete();
                log.info("[ChatService] 스트리밍 케어 진단 처리 완료 - userId={}, characterId={}", userId, characterId);
            } catch (Exception e) {
                log.warn("[ChatService] 스트리밍 케어 진단 실패 - userId={}, characterId={}, error={}",
                        userId, characterId, e.getMessage());
                sendChunk(emitter, careGuideFallback(character));
                emitter.complete();
            }
        });
    }

    // 파싱 실패해도 예외로 전체 요청을 막지 않고, 그냥 맥락 없이(빈 히스토리) 진행한다
    private List<ChatHistoryMessage> parseHistory(String historyJson) {
        if (historyJson == null || historyJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(ChatHistoryMessage.class).readValue(historyJson);
        } catch (IOException e) {
            log.warn("[ChatService] history JSON 파싱 실패 - {}", e.getMessage());
            return List.of();
        }
    }

    private String careGuideFallback(Character character) {
        return productRepository.findById(character.getProductId())
                .map(product -> CareGuideTemplate.guideFor(product.getMaterial()))
                .orElseGet(() -> CareGuideTemplate.guideFor(null));
    }

    // 보유(수집) 여부와 무관하게 캐릭터가 존재하기만 하면 대화 가능하다
    private Character resolveCharacter(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHARACTER_NOT_FOUND));
    }

    // history의 role(USER/CHARACTER)을 OpenAI 표기(user/assistant)로 변환. 알 수 없는 값은 안전하게 user로 취급
    private List<ChatTurn> toChatTurns(List<ChatHistoryMessage> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream()
                .map(turn -> "CHARACTER".equalsIgnoreCase(turn.getRole())
                        ? ChatTurn.assistant(turn.getContent())
                        : ChatTurn.user(turn.getContent()))
                .toList();
    }
}
