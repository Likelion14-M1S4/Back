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
            // collection.product_id 전역 유니크 제약(다른 유저가 같은 제품의 컬렉션 행을 이미 보유)과의
            // 충돌을 INSERT 전에 감지한다. 제약 위반을 catch하는 방식은 트랜잭션이 rollback-only로
            // 마킹돼 커밋에서 500이 나므로 쓸 수 없다. 충돌 시 시연 흐름이 끊기지 않게 성공으로 응답하되
            // 실제 저장은 안 된 것이므로 로그로 남긴다 (제약 재설계는 별도 논의)
            if (collectionRepository.existsByProductId(character.getProductId())) {
                log.warn("[CharacterService] 컬렉션 생성 스킵(product_id 유니크 선점) - userId={}, characterId={}",
                        userId, characterId);
            } else {
                // 실물 태그 = 소유 확인이므로 바로 OWNED로 생성
                collectionRepository.save(Collection.builder()
                        .userId(userId)
                        .characterId(characterId)
                        .productId(character.getProductId())
                        .status(CollectionStatus.OWNED)
                        .build());
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
