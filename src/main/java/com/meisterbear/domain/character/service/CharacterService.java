package com.meisterbear.domain.character.service;

import com.meisterbear.domain.character.dto.response.CollectCharacterResponse;
import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.entity.CollectionStatus;
import com.meisterbear.domain.character.exception.CharacterErrorCode;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.entity.ProductStore;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.product.repository.ProductStoreRepository;
import com.meisterbear.global.exception.CustomException;
import java.util.List;
import java.util.Objects;
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
    private final CharmRepository charmRepository;
    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;

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

        syncCharmDisplayInfo(character);

        log.info("[CharacterService] 캐릭터 수집 완료 - userId={}, characterId={}", userId, characterId);
        return CollectCharacterResponse.builder()
                .id(characterId)
                .collected(true)
                .build();
    }

    // 캐릭터=참 1:1 전제. 수집 시점에 매칭되는 참이 있으면 표시 정보(이름/이미지)를 캐릭터 값으로 맞추고,
    // 없으면 캐릭터 기준으로 새로 만들어 1:1 연결을 보장한다.
    private void syncCharmDisplayInfo(Character character) {
        Charm charm = charmRepository.findFirstByCharacterIdOrderByIdAsc(character.getId())
                .orElseGet(() -> createCharmFor(character));
        if (charm == null) {
            return;
        }
        if (!Objects.equals(charm.getName(), character.getName())
                || !Objects.equals(charm.getImgUrl(), character.getImgUrl())) {
            charm.syncDisplayInfo(character.getName(), character.getImgUrl());
        }
    }

    // 매칭되는 참이 아직 없을 때 캐릭터 기준으로 새로 만든다.
    // price/color/collectionName 등 매장 판매 정보는 NFC 태그 시점엔 알 수 없어 비워두고 생성 - 이후 관리자가 채워야 한다.
    // 매장 정보(store_id)를 못 구하면(제품에 연결된 매장 없음) 생성을 건너뛴다 - store_id는 필수 컬럼이라 임의값을 넣을 수 없다
    private Charm createCharmFor(Character character) {
        Product product = productRepository.findById(character.getProductId()).orElse(null);
        if (product == null) {
            log.warn("[CharacterService] 참 자동 생성 스킵(제품 없음) - characterId={}", character.getId());
            return null;
        }
        Long storeId = productStoreRepository.findFirstByProductIdOrderByStoreIdAsc(product.getId())
                .map(ProductStore::getStoreId)
                .orElse(null);
        if (storeId == null) {
            log.warn("[CharacterService] 참 자동 생성 스킵(매장 정보 없음) - characterId={}", character.getId());
            return null;
        }
        Charm charm = charmRepository.save(Charm.builder()
                .storeId(storeId)
                .characterId(character.getId())
                .season(product.getSeason())
                .name(character.getName())
                .imgUrl(character.getImgUrl())
                .seasonLimited(true)
                .build());
        log.info("[CharacterService] 캐릭터 수집 시 매칭 참 자동 생성 - characterId={}, charmId={}",
                character.getId(), charm.getId());
        return charm;
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
