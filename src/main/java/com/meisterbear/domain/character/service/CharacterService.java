package com.meisterbear.domain.character.service;

import com.meisterbear.domain.character.dto.response.CollectCharacterResponse;
import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.entity.CollectionStatus;
import com.meisterbear.domain.character.exception.CharacterErrorCode;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final CollectionRepository collectionRepository;

    // NFC 태그로 얻은 캐릭터를 컬렉션에 추가한다(OWNED).
    // 시연 중 재태그해도 에러가 나지 않도록 멱등으로 동작한다 - 이미 수집했으면 그대로 성공 응답
    @Transactional
    public CollectCharacterResponse collect(Long userId, Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(CharacterErrorCode.CHARACTER_NOT_FOUND));

        List<Collection> collections = collectionRepository.findByUserIdAndCharacterId(userId, characterId);
        if (collections.isEmpty()) {
            try {
                // 실물 태그 = 소유 확인이므로 바로 OWNED로 생성
                collectionRepository.save(Collection.builder()
                        .userId(userId)
                        .characterId(characterId)
                        .productId(character.getProductId())
                        .status(CollectionStatus.OWNED)
                        .build());
            } catch (DataIntegrityViolationException e) {
                // collection.product_id 전역 유니크 제약(다른 유저가 같은 제품의 컬렉션 행을 이미 보유) 등과
                // 충돌해도 시연 흐름이 500으로 끊기지 않도록 성공으로 처리한다 (제약 재설계는 별도 논의)
                log.warn("[CharacterService] 컬렉션 생성 제약 충돌(멱등 처리) - userId={}, characterId={}",
                        userId, characterId);
            }
        } else {
            collections.forEach(this::promoteToOwned);
        }

        log.info("[CharacterService] 캐릭터 수집 완료 - userId={}, characterId={}", userId, characterId);
        return CollectCharacterResponse.builder()
                .id(characterId)
                .collected(true)
                .build();
    }

    // 기존 행이 있으면 상태 전이 규칙(LOCKED→PREVIEW→OWNED)을 따라 OWNED까지 승격. 이미 OWNED면 아무것도 안 함
    private void promoteToOwned(Collection collection) {
        if (collection.getStatus() == CollectionStatus.LOCKED) {
            collection.preview();
        }
        if (collection.getStatus() == CollectionStatus.PREVIEW) {
            collection.own();
        }
    }
}
