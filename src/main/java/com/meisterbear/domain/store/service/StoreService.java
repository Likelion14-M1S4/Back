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

    // 파싱 실패 시 빈 목록으로 대체. product 도메인도 같은 파싱을 쓰므로 public
    public List<StoreHourResponse> parseHours(Store store) {
        String raw = store.getHours();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isArray()) {
                log.warn("[StoreService] 운영시간 JSON이 배열이 아님 - storeId={}", store.getId());
                return List.of();
            }
            List<StoreHourResponse> hours = new ArrayList<>();
            for (JsonNode node : root) {
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
