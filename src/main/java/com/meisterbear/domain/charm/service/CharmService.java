package com.meisterbear.domain.charm.service;

import com.meisterbear.domain.charm.dto.response.OwnedCharmGroupResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmListResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmResponse;
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.entity.CharmReceipt;
import com.meisterbear.domain.charm.entity.CharmReceiptStatus;
import com.meisterbear.domain.charm.repository.CharmReceiptRepository;
import com.meisterbear.domain.charm.repository.CharmRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharmService {

    private final CharmReceiptRepository charmReceiptRepository;
    private final CharmRepository charmRepository;

    public OwnedCharmListResponse findOwnedCharms(Long userId) {
        List<CharmReceipt> receipts = charmReceiptRepository.findByUserIdAndStatus(userId,
                CharmReceiptStatus.COMPLETED);
        if (receipts.isEmpty()) {
            log.info("[CharmService] 보유 참 목록 조회 완료(보유 참 없음) - userId={}", userId);
            return OwnedCharmListResponse.empty();
        }

        List<Long> charmIds = receipts.stream().map(CharmReceipt::getCharmId).distinct().toList();
        List<Charm> charms = charmRepository.findAllById(charmIds);

        Map<String, List<Charm>> charmsByCollection = charms.stream()
                .collect(Collectors.groupingBy(Charm::getCollectionName, LinkedHashMap::new, Collectors.toList()));

        List<OwnedCharmGroupResponse> collections = charmsByCollection.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entry -> OwnedCharmGroupResponse.builder()
                        .collectionName(entry.getKey())
                        .charms(entry.getValue().stream()
                                .map(charm -> OwnedCharmResponse.builder()
                                        .id(charm.getId())
                                        .name(charm.getName())
                                        .imgUrl(charm.getImgUrl())
                                        .collectionName(charm.getCollectionName())
                                        .build())
                                .toList())
                        .build())
                .toList();

        log.info("[CharmService] 보유 참 목록 조회 완료 - userId={}, collectionCount={}", userId, collections.size());
        return OwnedCharmListResponse.builder()
                .collections(collections)
                .build();
    }
}
