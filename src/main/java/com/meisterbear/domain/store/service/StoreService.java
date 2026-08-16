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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    // 구매 가능 매장 전체 목록. id 순 정렬을 명시한다 (findAll만으로는 순서가 보장되지 않음)
    public List<StoreResponse> getStores() {
        List<StoreResponse> stores = storeRepository.findAll(Sort.by("id")).stream()
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
    // 매장 목록 자체는 내려가야 하므로(시연 우선) 실패 시 빈 목록으로 대체한다.
    // 매장 태그 상세(product 도메인)도 같은 파싱을 쓰므로 public으로 둔다
    public List<StoreHourResponse> parseHours(Store store) {
        String raw = store.getHours();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            // 배열이 아닌 JSON(객체 등)은 readTree가 예외 없이 통과해 빈 값 항목을 만들 수 있으므로 구조를 검증한다
            if (!root.isArray()) {
                log.warn("[StoreService] 운영시간 JSON이 배열이 아님 - storeId={}", store.getId());
                return List.of();
            }
            List<StoreHourResponse> hours = new ArrayList<>();
            for (JsonNode node : root) {
                // 불량 항목 하나 때문에 정상 요일까지 사라지지 않도록, 해당 항목만 스킵하고 나머지는 보존한다
                if (!node.hasNonNull("day") || !node.hasNonNull("time")) {
                    log.warn("[StoreService] 운영시간 항목에 day/time 누락(해당 항목 스킵) - storeId={}", store.getId());
                    continue;
                }
                hours.add(StoreHourResponse.builder()
                        .day(node.get("day").asText())
                        .time(node.get("time").asText())
                        .build());
            }
            return hours;
        } catch (Exception e) {
            log.warn("[StoreService] 운영시간 JSON 파싱 실패 - storeId={}", store.getId());
            return List.of();
        }
    }
}
