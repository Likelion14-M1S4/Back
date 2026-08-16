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

    // 정품 인증서 조회. uid가 있으면 그 제품의 내 구매 기록, 없으면 최근 구매 1건 기준.
    // 인증서 필드는 구매 기록(order_item)과 제품(product)에서 그대로 채운다
    public CertificateResponse getCertificate(Long userId, String uid) {
        // uid 경로에서는 제품을 먼저 특정하므로, 조회한 Product를 재사용해 동일 행 중복 SELECT를 피한다
        Product product;
        OrderItem order;
        if (uid != null && !uid.isBlank()) {
            product = productRepository.findByNfcUid(uid)
                    .orElseThrow(() -> new CustomException(NfcErrorCode.NFC_NOT_FOUND));
            order = orderItemRepository.findFirstByUserIdAndProductIdOrderByOrderedAtDesc(userId, product.getId())
                    .orElseThrow(() -> new CustomException(NfcErrorCode.CERTIFICATE_NOT_FOUND));
        } else {
            order = orderItemRepository.findFirstByUserIdOrderByOrderedAtDesc(userId)
                    .orElseThrow(() -> new CustomException(NfcErrorCode.CERTIFICATE_NOT_FOUND));
            product = productRepository.findById(order.getProductId()).orElse(null);
        }
        String purchasePlace = storeRepository.findById(order.getStoreId())
                .map(Store::getName)
                .orElse(null);

        // 발급일은 보증서 발급일(warranty_issued_at)을 우선하고, 없으면 구매일로 대체
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

    // "2026.08.16 pm.03:00" - 프론트 화면이 쓰는 날짜+시간 표기 포맷 (12시간제, am/pm 소문자)
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        int hour = dateTime.getHour();
        String meridiem = hour < 12 ? "am" : "pm";
        int hour12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%s %s.%02d:%02d", dateTime.format(DATE_FORMAT), meridiem, hour12, dateTime.getMinute());
    }

    // 태그한 제품의 진열 매장으로 방문 이력(user_tag STORE)을 남긴다.
    // 매장 연결이 없는 제품이면 조용히 건너뛴다 - 이력은 부가 기능이라 태그 검증 자체를 막지 않는다.
    // 같은 유저·제품·매장은 하루 1회만 기록한다 (재태그 시 이력 상세의 날짜 그룹에 같은 제품이 중복 노출되는 것 방지)
    private void recordStoreTag(Long userId, Product product) {
        productStoreRepository.findFirstByProductIdOrderByStoreIdAsc(product.getId())
                .ifPresent(productStore -> {
                    LocalDateTime dayStart = LocalDate.now().atStartOfDay();
                    LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
                    boolean alreadyTaggedToday = userTagRepository
                            .existsByUserIdAndProductIdAndStoreIdAndTagTypeAndTaggedAtBetween(
                                    userId, product.getId(), productStore.getStoreId(), TagType.STORE,
                                    dayStart, dayEnd);
                    if (alreadyTaggedToday) {
                        return;
                    }
                    userTagRepository.save(UserTag.builder()
                            .userId(userId)
                            .productId(product.getId())
                            .tagType(TagType.STORE)
                            .storeId(productStore.getStoreId())
                            .build());
                });
    }

    // 제품에 연결된 캐릭터. 없으면 null (캐릭터 없는 제품도 태그/인증서는 정상 동작해야 한다)
    private NfcCharacterResponse findCharacter(Long productId) {
        Character character = characterRepository.findByProductId(productId).orElse(null);
        if (character == null) {
            return null;
        }
        String collectionName = charmRepository.findFirstByCharacterIdOrderByIdAsc(character.getId())
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
