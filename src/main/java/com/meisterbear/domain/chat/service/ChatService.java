package com.meisterbear.domain.chat.service;

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
import com.meisterbear.domain.user.entity.User;
import com.meisterbear.domain.user.exception.UserErrorCode;
import com.meisterbear.domain.user.repository.UserRepository;
import com.meisterbear.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final OpenAiClient openAiClient;

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
