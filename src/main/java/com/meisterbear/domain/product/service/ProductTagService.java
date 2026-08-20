package com.meisterbear.domain.product.service;

import com.meisterbear.domain.product.dto.response.StoreTagDetailResponse;
import com.meisterbear.domain.product.dto.response.StoreTagHistoryResponse;
import com.meisterbear.domain.product.dto.response.TaggedGroupResponse;
import com.meisterbear.domain.product.dto.response.TaggedProductResponse;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.entity.TagType;
import com.meisterbear.domain.product.entity.UserTag;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.product.repository.UserTagRepository;
import com.meisterbear.domain.store.entity.Store;
import com.meisterbear.domain.store.exception.StoreErrorCode;
import com.meisterbear.domain.store.repository.StoreRepository;
import com.meisterbear.domain.store.service.StoreService;
import com.meisterbear.global.exception.CustomException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductTagService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final UserTagRepository userTagRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;

    public List<StoreTagHistoryResponse> getStoreTagHistory(Long userId) {
        List<UserTag> tags = userTagRepository.findByUserIdAndTagTypeOrderByTaggedAtDesc(userId, TagType.STORE);

        // LinkedHashMap으로 최근 방문 매장 순을 보존
        Map<Long, LocalDateTime> lastVisitedByStore = new LinkedHashMap<>();
        for (UserTag tag : tags) {
            if (tag.getStoreId() != null) {
                lastVisitedByStore.putIfAbsent(tag.getStoreId(), tag.getTaggedAt());
            }
        }

        Map<Long, Store> stores = storeRepository.findAllById(lastVisitedByStore.keySet()).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));

        List<StoreTagHistoryResponse> responses = lastVisitedByStore.entrySet().stream()
                .map(entry -> {
                    Store store = stores.get(entry.getKey());
                    return StoreTagHistoryResponse.builder()
                            .id(entry.getKey())
                            .storeName(store != null ? store.getName() : null)
                            .lastVisitedAt(entry.getValue().format(DATE_FORMAT))
                            .build();
                })
                .toList();
        log.info("[ProductTagService] 매장 태그 이력 목록 조회 완료 - userId={}, stores={}", userId, responses.size());
        return responses;
    }

    public StoreTagDetailResponse getStoreTagDetail(Long userId, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

        List<UserTag> tags =
                userTagRepository.findByUserIdAndStoreIdAndTagTypeOrderByTaggedAtDesc(userId, storeId, TagType.STORE);
        Map<Long, Product> products = productRepository.findAllById(
                        tags.stream().map(UserTag::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<LocalDate, List<TaggedProductResponse>> groups = new LinkedHashMap<>();
        for (UserTag tag : tags) {
            Product product = products.get(tag.getProductId());
            groups.computeIfAbsent(tag.getTaggedAt().toLocalDate(), key -> new ArrayList<>())
                    .add(TaggedProductResponse.builder()
                            .id(tag.getProductId())
                            .name(product != null ? product.getName() : null)
                            .imageUrl(product != null ? product.getImgUrl() : null)
                            .build());
        }

        List<TaggedGroupResponse> taggedGroups = groups.entrySet().stream()
                .map(entry -> TaggedGroupResponse.builder()
                        .date(entry.getKey().format(DATE_FORMAT))
                        .products(entry.getValue())
                        .build())
                .toList();

        String address = store.getPostalCode() != null
                ? store.getAddress() + " " + store.getPostalCode()
                : store.getAddress();

        log.info("[ProductTagService] 매장 태그 상세 조회 완료 - userId={}, storeId={}, groups={}",
                userId, storeId, taggedGroups.size());
        return StoreTagDetailResponse.builder()
                .storeName(store.getName())
                .address(address)
                .phone(store.getPhone())
                .hours(storeService.parseHours(store))
                .taggedGroups(taggedGroups)
                .build();
    }
}
