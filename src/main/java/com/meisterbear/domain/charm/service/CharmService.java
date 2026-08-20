package com.meisterbear.domain.charm.service;

import com.meisterbear.domain.charm.dto.response.CharmDetailResponse;
import com.meisterbear.domain.charm.dto.response.CharmListResponse;
import com.meisterbear.domain.charm.dto.response.CharmRecommendationResponse;
import com.meisterbear.domain.charm.dto.response.CharmSummaryResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmGroupResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmListResponse;
import com.meisterbear.domain.charm.dto.response.OwnedCharmResponse;
import com.meisterbear.domain.charm.dto.response.PurchasableCharmGroupResponse;
import com.meisterbear.domain.charm.dto.response.PurchasableCharmListResponse;
import com.meisterbear.domain.charm.dto.response.PurchasableCharmResponse;
import com.meisterbear.domain.charm.dto.response.RecommendedCharmResponse;
import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.entity.CharmReceipt;
import com.meisterbear.domain.charm.entity.CharmReceiptStatus;
import com.meisterbear.domain.charm.exception.CharmErrorCode;
import com.meisterbear.domain.charm.repository.CharmReceiptRepository;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.character.entity.CollectionStatus;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.story.service.StoryService;
import com.meisterbear.global.exception.CustomException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharmService {

    // collection_name이 비어있는 참을 묶는 그룹명 (groupingBy가 null을 못 받아서 치환)
    private static final String UNCATEGORIZED_COLLECTION_NAME = "기타";

    private final CharmReceiptRepository charmReceiptRepository;
    private final CharmRepository charmRepository;
    private final CollectionRepository collectionRepository;
    private final StoryService storyService;

    // 수집(collect)한 캐릭터의 참만 id 오름차순으로 반환
    public CharmListResponse findAllCharms(Long userId) {
        List<Long> ownedCharacterIds = collectionRepository.findCharacterIdsByUserIdAndStatus(
                userId, CollectionStatus.OWNED);
        if (ownedCharacterIds.isEmpty()) {
            log.info("[CharmService] 참 목록 조회 완료(수집한 캐릭터 없음) - userId={}", userId);
            return CharmListResponse.empty();
        }

        List<Charm> charms = charmRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(charm -> ownedCharacterIds.contains(charm.getCharacterId()))
                .toList();
        if (charms.isEmpty()) {
            log.info("[CharmService] 참 목록 조회 완료(매칭되는 참 없음) - userId={}", userId);
            return CharmListResponse.empty();
        }

        List<CharmSummaryResponse> summaries = charms.stream()
                .map(charm -> CharmSummaryResponse.builder()
                        .id(charm.getId())
                        .characterId(charm.getCharacterId())
                        .name(charm.getName())
                        .imgUrl(charm.getImgUrl())
                        .collectionName(charm.getCollectionName())
                        .build())
                .toList();

        log.info("[CharmService] 참 목록 조회 완료 - userId={}, count={}", userId, summaries.size());
        return CharmListResponse.builder()
                .charms(summaries)
                .build();
    }

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
                .collect(Collectors.groupingBy(CharmService::resolveCollectionName, LinkedHashMap::new,
                        Collectors.toList()));

        List<OwnedCharmGroupResponse> collections = charmsByCollection.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entry -> OwnedCharmGroupResponse.builder()
                        .collectionName(entry.getKey())
                        .charms(entry.getValue().stream()
                                .map(charm -> OwnedCharmResponse.builder()
                                        .id(charm.getId())
                                        .characterId(charm.getCharacterId())
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

    // 시즌 완주 여부로 구매 가능 판단, 이미 수령한 참은 제외
    public PurchasableCharmListResponse findPurchasableCharms(Long userId) {
        if (!storyService.isCurrentSeasonCompleted(userId)) {
            log.info("[CharmService] 구매 가능한 참 목록 조회 완료(시즌 미완주) - userId={}", userId);
            return PurchasableCharmListResponse.empty();
        }

        Set<Long> ownedCharmIds = charmReceiptRepository.findByUserIdAndStatus(userId, CharmReceiptStatus.COMPLETED)
                .stream()
                .map(CharmReceipt::getCharmId)
                .collect(Collectors.toSet());

        List<Charm> charms = charmRepository.findAll().stream()
                .filter(charm -> !ownedCharmIds.contains(charm.getId()))
                .toList();
        if (charms.isEmpty()) {
            log.info("[CharmService] 구매 가능한 참 목록 조회 완료(구매 가능한 참 없음) - userId={}", userId);
            return PurchasableCharmListResponse.empty();
        }

        Map<String, List<Charm>> charmsByCollection = charms.stream()
                .collect(Collectors.groupingBy(CharmService::resolveCollectionName, LinkedHashMap::new,
                        Collectors.toList()));

        List<PurchasableCharmGroupResponse> collections = charmsByCollection.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entry -> PurchasableCharmGroupResponse.builder()
                        .collectionName(entry.getKey())
                        .charms(entry.getValue().stream()
                                .map(charm -> PurchasableCharmResponse.builder()
                                        .id(charm.getId())
                                        .characterId(charm.getCharacterId())
                                        .name(charm.getName())
                                        .price(charm.getPrice())
                                        .color(charm.getColor())
                                        .imgUrl(charm.getImgUrl())
                                        .collectionName(charm.getCollectionName())
                                        .build())
                                .toList())
                        .build())
                .toList();

        log.info("[CharmService] 구매 가능한 참 목록 조회 완료 - userId={}, collectionCount={}", userId, collections.size());
        return PurchasableCharmListResponse.builder()
                .collections(collections)
                .build();
    }

    public CharmDetailResponse findCharmDetail(Long userId, Long charmId) {
        Charm charm = charmRepository.findById(charmId)
                .orElseThrow(() -> new CustomException(CharmErrorCode.CHARM_NOT_FOUND));

        CharmDetailResponse response = toCharmDetailResponse(charm);
        log.info("[CharmService] 참 상세 조회 완료 - userId={}, charmId={}", userId, charmId);
        return response;
    }

    // 참 추천 - 상단엔 선택한 참 상세, 하단엔 이 참을 제외한 전체 참 목록을 반환
    public CharmRecommendationResponse findCharmRecommendations(Long userId, Long charmId) {
        Charm charm = charmRepository.findById(charmId)
                .orElseThrow(() -> new CustomException(CharmErrorCode.CHARM_NOT_FOUND));

        List<Charm> otherCharms = charmRepository.findByIdNot(charm.getId());
        List<RecommendedCharmResponse> recommendations = otherCharms.stream()
                .map(similar -> RecommendedCharmResponse.builder()
                        .id(similar.getId())
                        .characterId(similar.getCharacterId())
                        .name(similar.getName())
                        .imgUrl(similar.getImgUrl())
                        .collectionName(similar.getCollectionName())
                        .build())
                .toList();

        log.info("[CharmService] 참 추천 조회 완료 - userId={}, charmId={}, recommendationCount={}",
                userId, charmId, recommendations.size());
        return CharmRecommendationResponse.builder()
                .charm(toCharmDetailResponse(charm))
                .recommendations(recommendations)
                .build();
    }

    private static String resolveCollectionName(Charm charm) {
        return Objects.requireNonNullElse(charm.getCollectionName(), UNCATEGORIZED_COLLECTION_NAME);
    }

    private CharmDetailResponse toCharmDetailResponse(Charm charm) {
        return CharmDetailResponse.builder()
                .id(charm.getId())
                .characterId(charm.getCharacterId())
                .name(charm.getName())
                .price(charm.getPrice())
                .color(charm.getColor())
                .imgUrl(charm.getImgUrl())
                .description(charm.getDescription())
                .collectionName(charm.getCollectionName())
                .build();
    }
}
