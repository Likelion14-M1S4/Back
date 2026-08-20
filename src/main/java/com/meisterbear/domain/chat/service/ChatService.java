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
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.repository.CharmRepository;
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

// 클래스 레벨 @Transactional 없음 - OpenAI 호출 동안 DB 커넥션을 오래 잡지 않기 위해
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // 진입 화면 고정 선택지 - care를 고르면 프론트가 이후 사진 업로드를 /inspector로 보냄
    private static final List<StarterChoiceResponse> STARTER_CHOICES = List.of(
            StarterChoiceResponse.builder().id(1L).label("제품이 오염됐어").tagName("care").build(),
            StarterChoiceResponse.builder().id(2L).label("너에 대해 알고싶어").tagName("character").build()
    );

    // SseEmitter를 즉시 반환해야 해서 OpenAI 호출은 가상 스레드에서 돌린다
    private static final ExecutorService STREAM_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CharmRepository charmRepository;
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
                .charmId(resolveCharmId(character.getId()))
                .characterName(character.getName())
                .characterImgUrl(character.getImgUrl())
                .greeting("안녕하세요, " + user.getNickname() + "님. 어떤 얘기를 나눠볼까요?")
                .starterChoices(STARTER_CHOICES)
                .build();
    }

    // AI 호출 실패 시 대체 문구로 항상 200을 반환한다
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
                .charmId(resolveCharmId(character.getId()))
                .reply(reply)
                .build();
    }

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

    // 청크 전송 실패로 스트림 전체가 끊기지 않게 여기서 흡수한다
    private void sendChunk(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (IOException e) {
            log.warn("[ChatService] SSE 청크 전송 실패 - error={}", e.getMessage());
        }
    }

    // history는 multipart 파트라 JSON 문자열로 받는다. 분석 실패 시 소재 기준 케어 가이드로 대체한다.
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
                .charmId(resolveCharmId(character.getId()))
                .reply(reply)
                .build();
    }

    // 이미지 바이트는 비동기 스레드로 넘어가기 전에 미리 읽어둔다 (요청 종료 후엔 임시 파일이 정리될 수 있음)
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

    private Character resolveCharacter(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHARACTER_NOT_FOUND));
    }

    private Long resolveCharmId(Long characterId) {
        return charmRepository.findFirstByCharacterIdOrderByIdAsc(characterId)
                .map(Charm::getId)
                .orElse(null);
    }

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
