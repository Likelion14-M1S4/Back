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
import com.meisterbear.domain.character.entity.CollectionStatus;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.user.entity.User;
import com.meisterbear.domain.user.exception.UserErrorCode;
import com.meisterbear.domain.user.repository.UserRepository;
import com.meisterbear.global.exception.CustomException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    // 진입 화면에 항상 고정으로 노출되는 대화 시작 선택지 - care를 고르면 프론트가 이후 사진 업로드를 /inspector로 보냄
    private static final List<StarterChoiceResponse> STARTER_CHOICES = List.of(
            StarterChoiceResponse.builder().id(1L).label("제품이 오염됐어").tagName("care").build(),
            StarterChoiceResponse.builder().id(2L).label("이 제품에 대해 알려줘").tagName("product").build(),
            StarterChoiceResponse.builder().id(3L).label("너에 대해 알고싶어").tagName("character").build()
    );

    private final CharacterRepository characterRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public ChatEntryResponse findEntry(Long userId, Long characterId) {
        Character character = resolveOwnedCharacter(userId, characterId);

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
        Character character = resolveOwnedCharacter(userId, request.getCharacterId());

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

    // 케어 문의 - 사진을 분석해서 관찰 내용/권장 케어를 대화체로 돌려준다. 텍스트는 안 받고 사진만 받는다
    // (직전까지 나눈 대화 맥락은 history로 이어받는다 - 대화 기록 미저장 정책이라 프론트가 매번 실어 보내야 함).
    // history는 multipart 파트라 JSON 문자열로 받는다 - @RequestPart로 객체를 바로 받으면 Swagger 등 일부
    // 클라이언트가 Content-Type을 application/json으로 안 붙여줘서 415가 나는 문제가 있었음.
    // 분석이 실패해도 오류를 던지지 않고, 캐릭터가 속한 제품의 소재 기준 고정 케어 가이드로 대체해 항상 200을 반환한다.
    public ChatMessageResultResponse inspect(Long userId, Long characterId, String historyJson,
                                              MultipartFile image) {
        Character character = resolveOwnedCharacter(userId, characterId);

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

    private Character resolveOwnedCharacter(Long userId, Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHARACTER_NOT_FOUND));

        boolean owned = collectionRepository.existsByUserIdAndCharacterIdAndStatus(
                userId, characterId, CollectionStatus.OWNED);
        if (!owned) {
            throw new CustomException(ChatErrorCode.CHARACTER_NOT_OWNED);
        }
        return character;
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
