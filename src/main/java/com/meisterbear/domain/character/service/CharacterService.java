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
    private final CharmRepository charmRepository;
    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;

    // NFC 태그로 얻은 캐릭터를 컬렉션에 추가한다(OWNED). 이미 수집했어도 멱등하게 성공 응답한다.
    @Transactional
    public CollectCharacterResponse collect(Long userId, Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(CharacterErrorCode.CHARACTER_NOT_FOUND));

        List<Collection> collections = collectionRepository.findByUserIdAndCharacterId(userId, characterId);
        if (collections.isEmpty()) {
            collectionRepository.save(Collection.builder()
                    .userId(userId)
                    .characterId(characterId)
                    .productId(character.getProductId())
                    .status(CollectionStatus.OWNED)
                    .build());
        } else {
            collections.forEach(this::promoteToOwned);
        }

        Charm charm = syncCharmDisplayInfo(character);

        log.info("[CharacterService] 캐릭터 수집 완료 - userId={}, characterId={}", userId, characterId);
        return CollectCharacterResponse.builder()
                .id(characterId)
                .charmId(charm != null ? charm.getId() : null)
                .collected(true)
                .build();
    }

    // 캐릭터=참 1:1. 이름만 캐릭터 값으로 맞추고 이미지는 동기화하지 않는다.
    private Charm syncCharmDisplayInfo(Character character) {
        Charm charm = charmRepository.findFirstByCharacterIdOrderByIdAsc(character.getId())
                .orElseGet(() -> createCharmFor(character));
        if (charm == null) {
            return null;
        }
        if (!Objects.equals(charm.getName(), character.getName())) {
            charm.syncName(character.getName());
        }
        return charm;
    }

    // price/color 등 판매 정보는 비워두고 생성(이후 관리자가 채움), 매장 정보 없으면 스킵
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
        try {
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
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 충돌 시 이미 생성된 참을 재조회
            log.warn("[CharacterService] 참 동시 생성 충돌 - 기존 참 재조회 - characterId={}", character.getId());
            return charmRepository.findFirstByCharacterIdOrderByIdAsc(character.getId()).orElseThrow(() -> e);
        }
    }

    // LOCKED→PREVIEW→OWNED까지 승격
    private void promoteToOwned(Collection collection) {
        if (collection.getStatus() == CollectionStatus.LOCKED) {
            collection.preview();
        }
        if (collection.getStatus() == CollectionStatus.PREVIEW) {
            collection.own();
        }
    }
}
