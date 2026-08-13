package com.meisterbear.domain.charm.service;

import com.meisterbear.domain.charm.dto.response.CharmDetailResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmGroupResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmListResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmResponse;
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.entity.CharmReceipt;
import com.meisterbear.domain.charm.entity.CharmReceiptStatus;
import com.meisterbear.domain.charm.exception.CharmErrorCode;
import com.meisterbear.domain.charm.repository.CharmReceiptRepository;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.story.dto.response.StoryListResponse;
import com.meisterbear.domain.story.service.StoryService;
import com.meisterbear.global.exception.CustomException;
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
    private final StoryService storyService;

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

    // 시즌 한정 참 상세 - 구매 가능 여부는 이 유저의 현재 시즌 스토리 전체 완주 여부로 판단
    public CharmDetailResponse findCharmDetail(Long userId, Long charmId) {
        Charm charm = charmRepository.findById(charmId)
                .orElseThrow(() -> new CustomException(CharmErrorCode.CHARM_NOT_FOUND));

        StoryListResponse stories = storyService.findStories(userId);
        boolean purchasable = stories.getCurrentSeason() != null && stories.getCurrentSeason().isAllCompleted();

        log.info("[CharmService] 시즌 한정 참 상세 조회 완료 - userId={}, charmId={}, isPurchasable={}",
                userId, charmId, purchasable);
        return CharmDetailResponse.builder()
                .id(charm.getId())
                .name(charm.getName())
                .price(charm.getPrice())
                .color(charm.getColor())
                .imgUrl(charm.getImgUrl())
                .isPurchasable(purchasable)
                .build();
    }
}
