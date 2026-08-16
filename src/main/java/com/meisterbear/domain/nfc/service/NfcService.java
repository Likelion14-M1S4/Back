package com.meisterbear.domain.nfc.service;

import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.nfc.dto.response.NfcCharacterResponse;
import com.meisterbear.domain.nfc.dto.response.NfcVerifyResponse;
import com.meisterbear.domain.nfc.exception.NfcErrorCode;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.entity.TagType;
import com.meisterbear.domain.product.entity.UserTag;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.product.repository.ProductStoreRepository;
import com.meisterbear.domain.product.repository.UserTagRepository;
import com.meisterbear.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NfcService {

    // 검증 후 프론트가 이동할 경로 (정품 인증서 화면)
    private static final String VERIFY_NEXT_PATH = "/store-tag/certificate";
    private static final String TYPE_PRODUCT = "PRODUCT";

    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;
    private final UserTagRepository userTagRepository;
    private final CharacterRepository characterRepository;
    private final CharmRepository charmRepository;

    // NFC 태그 검증(제품 태그). 제품·캐릭터 정보를 내려주고, 부수효과로 방문 매장 태그 이력을 기록한다.
    // 실물 NFC에 각인된 URL의 uid가 그대로 들어온다
    @Transactional
    public NfcVerifyResponse verify(Long userId, String uid) {
        Product product = productRepository.findByNfcUid(uid)
                .orElseThrow(() -> new CustomException(NfcErrorCode.NFC_NOT_FOUND));

        recordStoreTag(userId, product);

        log.info("[NfcService] NFC 태그 검증 완료 - userId={}, productId={}", userId, product.getId());
        return NfcVerifyResponse.builder()
                .type(TYPE_PRODUCT)
                .productId(product.getId())
                .productName(product.getName())
                .character(findCharacter(product.getId()))
                .nextPath(VERIFY_NEXT_PATH)
                .build();
    }

    // 태그한 제품의 진열 매장으로 방문 이력(user_tag STORE)을 남긴다.
    // 매장 연결이 없는 제품이면 조용히 건너뛴다 - 이력은 부가 기능이라 태그 검증 자체를 막지 않는다
    private void recordStoreTag(Long userId, Product product) {
        productStoreRepository.findFirstByProductId(product.getId())
                .ifPresent(productStore -> userTagRepository.save(UserTag.builder()
                        .userId(userId)
                        .productId(product.getId())
                        .tagType(TagType.STORE)
                        .storeId(productStore.getStoreId())
                        .build()));
    }

    // 제품에 연결된 캐릭터. 없으면 null (캐릭터 없는 제품도 태그/인증서는 정상 동작해야 한다)
    private NfcCharacterResponse findCharacter(Long productId) {
        Character character = characterRepository.findByProductId(productId).orElse(null);
        if (character == null) {
            return null;
        }
        String collectionName = charmRepository.findFirstByCharacterId(character.getId())
                .map(Charm::getCollectionName)
                .orElse(null);
        return NfcCharacterResponse.builder()
                .id(character.getId())
                .name(character.getName())
                .collectionName(collectionName)
                .description(character.getIntro())
                .imageUrl(character.getImgUrl())
                .build();
    }
}
