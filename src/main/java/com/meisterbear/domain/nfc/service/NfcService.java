package com.meisterbear.domain.nfc.service;

import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.nfc.dto.response.CertificateResponse;
import com.meisterbear.domain.nfc.dto.response.NfcCharacterResponse;
import com.meisterbear.domain.nfc.dto.response.NfcVerifyResponse;
import com.meisterbear.domain.nfc.exception.NfcErrorCode;
import com.meisterbear.domain.order.entity.OrderItem;
import com.meisterbear.domain.order.repository.OrderItemRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.entity.TagType;
import com.meisterbear.domain.product.entity.UserTag;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.product.repository.ProductStoreRepository;
import com.meisterbear.domain.product.repository.UserTagRepository;
import com.meisterbear.domain.store.entity.Store;
import com.meisterbear.domain.store.repository.StoreRepository;
import com.meisterbear.global.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String CERTIFICATE_SELLER_DEFAULT = "엠씨엠코리아";

    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;
    private final UserTagRepository userTagRepository;
    private final CharacterRepository characterRepository;
    private final CharmRepository charmRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;

    // 비로그인이면 이력 기록만 생략한다
    @Transactional
    public NfcVerifyResponse verify(Long userId, String uid) {
        Product product = productRepository.findByNfcUid(uid)
                .orElseThrow(() -> new CustomException(NfcErrorCode.NFC_NOT_FOUND));

        if (userId != null) {
            recordStoreTag(userId, product);
        }

        log.info("[NfcService] NFC 태그 검증 완료 - userId={}, productId={}", userId, product.getId());
        return NfcVerifyResponse.builder()
                .type(TYPE_PRODUCT)
                .productId(product.getId())
                .productName(product.getName())
                .character(findCharacter(product))
                .nextPath(VERIFY_NEXT_PATH)
                .build();
    }

    // uid 있으면 그 실물의 최신 구매 기록(유저 무관), 없으면 로그인 유저의 최근 구매 1건
    public CertificateResponse getCertificate(Long userId, String uid) {
        Product product;
        OrderItem order;
        if (uid != null && !uid.isBlank()) {
            product = productRepository.findByNfcUid(uid)
                    .orElseThrow(() -> new CustomException(NfcErrorCode.NFC_NOT_FOUND));
            order = orderItemRepository.findFirstByProductIdOrderByOrderedAtDescIdDesc(product.getId())
                    .orElseThrow(() -> new CustomException(NfcErrorCode.CERTIFICATE_NOT_FOUND));
        } else {
            if (userId == null) {
                throw new CustomException(NfcErrorCode.CERTIFICATE_NOT_FOUND);
            }
            order = orderItemRepository.findFirstByUserIdOrderByOrderedAtDescIdDesc(userId)
                    .orElseThrow(() -> new CustomException(NfcErrorCode.CERTIFICATE_NOT_FOUND));
            product = productRepository.findById(order.getProductId()).orElse(null);
        }
        String purchasePlace = storeRepository.findById(order.getStoreId())
                .map(Store::getName)
                .orElse(null);

        LocalDate issuedAt = order.getWarrantyIssuedAt() != null
                ? order.getWarrantyIssuedAt()
                : order.getOrderedAt().toLocalDate();

        log.info("[NfcService] 정품 인증서 조회 완료 - userId={}, orderItemId={}", userId, order.getId());
        return CertificateResponse.builder()
                .productName(product != null ? product.getName() : null)
                .imageUrl(product != null ? product.getImgUrl() : null)
                .orderNumber(order.getOrderNo())
                .productNumber(product != null ? product.getSerialNo() : null)
                .issuedAt(issuedAt.format(DATE_FORMAT))
                .purchasedAt(formatDateTime(order.getOrderedAt()))
                .receivedAt(formatDateTime(order.getReceivedAt()))
                .seller(order.getSeller() != null && !order.getSeller().isBlank()
                        ? order.getSeller() : CERTIFICATE_SELLER_DEFAULT)
                .purchasePlace(purchasePlace)
                .build();
    }

    // "2026.08.16 pm.03:00" 형식
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        int hour = dateTime.getHour();
        String meridiem = hour < 12 ? "am" : "pm";
        int hour12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%s %s.%02d:%02d", dateTime.format(DATE_FORMAT), meridiem, hour12, dateTime.getMinute());
    }

    // 매장 연결이 없으면 건너뛴다. 같은 유저·제품·매장은 하루 1회만 기록(UQ_USER_TAG_DAILY가 최종 방어선).
    private void recordStoreTag(Long userId, Product product) {
        productStoreRepository.findFirstByProductIdOrderByStoreIdAsc(product.getId())
                .ifPresent(productStore -> {
                    LocalDateTime dayStart = LocalDate.now().atStartOfDay();
                    boolean alreadyTaggedToday = userTagRepository
                            .existsByUserIdAndProductIdAndStoreIdAndTagTypeAndTaggedAtGreaterThanEqual(
                                    userId, product.getId(), productStore.getStoreId(), TagType.STORE,
                                    dayStart);
                    if (alreadyTaggedToday) {
                        return;
                    }
                    try {
                        userTagRepository.save(UserTag.builder()
                                .userId(userId)
                                .productId(product.getId())
                                .tagType(TagType.STORE)
                                .storeId(productStore.getStoreId())
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        log.warn("[NfcService] 매장 태그 중복 기록 시도 무시 - userId={}, productId={}", userId, product.getId());
                    }
                });
    }

    // 제품에 연결된 캐릭터. 없으면 null
    private NfcCharacterResponse findCharacter(Product taggedProduct) {
        Character character = characterRepository.findByProductId(taggedProduct.getId()).orElse(null);
        if (character == null) {
            return null;
        }
        String collectionName = resolveCollectionName(character.getId(), taggedProduct.getSeason());
        Long charmId = charmRepository.findFirstByCharacterIdOrderByIdAsc(character.getId())
                .map(Charm::getId)
                .orElse(null);
        return NfcCharacterResponse.builder()
                .id(character.getId())
                .charmId(charmId)
                .name(character.getName())
                .collectionName(collectionName)
                .description(character.getIntro())
                .imageUrl(character.getImgUrl())
                .build();
    }

    // 시즌 일치 참을 우선하고, 없으면 시즌 무관 첫 참으로 폴백
    private String resolveCollectionName(Long characterId, String season) {
        if (season != null && !season.isBlank()) {
            String seasonalName = charmRepository
                    .findFirstByCharacterIdAndSeasonOrderByIdAsc(characterId, season)
                    .map(Charm::getCollectionName)
                    .orElse(null);
            if (seasonalName != null && !seasonalName.isBlank()) {
                return seasonalName;
            }
        }
        return charmRepository.findFirstByCharacterIdOrderByIdAsc(characterId)
                .map(Charm::getCollectionName)
                .orElse(null);
    }
}
