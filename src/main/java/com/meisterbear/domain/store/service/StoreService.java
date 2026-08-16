package com.meisterbear.domain.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meisterbear.domain.store.dto.response.StoreHourResponse;
import com.meisterbear.domain.store.dto.response.StoreResponse;
import com.meisterbear.domain.store.entity.Store;
import com.meisterbear.domain.store.repository.StoreRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    // 구매 가능 매장 전체 목록. 매장 수가 적은 시연용 데이터라 필터/정렬 없이 id 순으로 반환한다
    public List<StoreResponse> getStores() {
        List<StoreResponse> stores = storeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        log.info("[StoreService] 매장 목록 조회 완료 - count={}", stores.size());
        return stores;
    }

    private StoreResponse toResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .postalCode(store.getPostalCode())
                .phone(store.getPhone())
                .hours(parseHours(store))
                .build();
    }

    // hours JSON 문자열 → [{day,time}] 목록. 시드 데이터 오타 등으로 파싱이 깨져도
    // 매장 목록 자체는 내려가야 하므로(시연 우선) 실패 시 빈 목록으로 대체한다
    private List<StoreHourResponse> parseHours(Store store) {
        String raw = store.getHours();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            List<StoreHourResponse> hours = new ArrayList<>();
            for (JsonNode node : root) {
                hours.add(StoreHourResponse.builder()
                        .day(node.path("day").asText())
                        .time(node.path("time").asText())
                        .build());
            }
            return hours;
        } catch (Exception e) {
            log.warn("[StoreService] 운영시간 JSON 파싱 실패 - storeId={}", store.getId());
            return List.of();
        }
    }
}
