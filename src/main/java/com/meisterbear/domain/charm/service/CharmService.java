package com.meisterbear.domain.charm.service;

import com.meisterbear.domain.charm.dto.response.CharmCharacterResponse;
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
import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.exception.CharacterErrorCode;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.story.service.StoryService;
import com.meisterbear.global.exception.CustomException;
import java.util.Comparator;
import java.util.HashMap;
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

    // collection_name이 비어있는 참을 묶는 그룹명. groupingBy는 classifier가 null을 반환하면
    // NullPointerException을 던지므로, 그룹핑 전에 항상 이 기본값으로 치환한다.
    private static final String UNCATEGORIZED_COLLECTION_NAME = "기타";

    private final CharmReceiptRepository charmReceiptRepository;
    private final CharmRepository charmRepository;
    private final CharacterRepository characterRepository;
    private final StoryService storyService;

    // 전체 참 목록 - id 오름차순으로 그룹핑 없이 카드 형태로 반환
    public CharmListResponse findAllCharms() {
        List<Charm> charms = charmRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (charms.isEmpty()) {
            log.info("[CharmService] 참 목록 조회 완료(참 없음)");
            return CharmListResponse.empty();
        }

        List<CharmSummaryResponse> summaries = charms.stream()
                .map(charm -> CharmSummaryResponse.builder()
                        .id(charm.getId())
                        .name(charm.getName())
                        .imgUrl(charm.getImgUrl())
                        .collectionName(charm.getCollectionName())
                        .build())
                .toList();

        log.info("[CharmService] 참 목록 조회 완료 - count={}", summaries.size());
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

    // 구매(수령) 가능한 참 목록 - 참마다 연결된 캐릭터×시즌 스토리를 이 유저가 전부 완주했는지로 판단.
    // 이미 수령 완료(COMPLETED)한 참은 제외하고, 완주한 참만 컬렉션명 기준으로 그룹핑해서 반환
    public PurchasableCharmListResponse findPurchasableCharms(Long userId) {
        Set<Long> ownedCharmIds = charmReceiptRepository.findByUserIdAndStatus(userId, CharmReceiptStatus.COMPLETED)
                .stream()
                .map(CharmReceipt::getCharmId)
                .collect(Collectors.toSet());

        List<Charm> candidates = charmRepository.findAll().stream()
                .filter(Charm::isSeasonLimited)
                .filter(charm -> !ownedCharmIds.contains(charm.getId()))
                .toList();

        // (characterId, season) 조합별로 완주 여부를 한 번만 조회해서 참마다 중복 쿼리가 나가지 않게 캐싱
        Map<String, Boolean> seasonCompletedCache = new HashMap<>();
        List<Charm> charms = candidates.stream()
                .filter(charm -> seasonCompletedCache.computeIfAbsent(
                        charm.getCharacterId() + ":" + charm.getSeason(),
                        key -> storyService.isSeasonCompleted(userId, charm.getCharacterId(), charm.getSeason())))
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

    // 참 상세 - isSeasonLimited에 따라 구매 가능 여부(시즌 한정) 또는 캐릭터 정보(일반)를 채워서 반환
    public CharmDetailResponse findCharmDetail(Long userId, Long charmId) {
        Charm charm = charmRepository.findById(charmId)
                .orElseThrow(() -> new CustomException(CharmErrorCode.CHARM_NOT_FOUND));

        CharmDetailResponse response = toCharmDetailResponse(userId, charm);
        log.info("[CharmService] 참 상세 조회 완료 - userId={}, charmId={}, isSeasonLimited={}, isPurchasable={}",
                userId, charmId, response.isSeasonLimited(), response.getPurchasable());
        return response;
    }

    // 참 추천 - 상단엔 선택한 참 상세, 하단엔 같은 collection_name×season(=같은 시즌 참 장식)의 나머지 참들을 반환
    public CharmRecommendationResponse findCharmRecommendations(Long userId, Long charmId) {
        Charm charm = charmRepository.findById(charmId)
                .orElseThrow(() -> new CustomException(CharmErrorCode.CHARM_NOT_FOUND));

        List<Charm> similarCharms = charmRepository
                .findByCollectionNameAndSeasonAndIdNot(charm.getCollectionName(), charm.getSeason(), charm.getId());
        List<RecommendedCharmResponse> recommendations = similarCharms.stream()
                .map(similar -> RecommendedCharmResponse.builder()
                        .id(similar.getId())
                        .name(similar.getName())
                        .imgUrl(similar.getImgUrl())
                        .collectionName(similar.getCollectionName())
                        .build())
                .toList();

        log.info("[CharmService] 참 추천 조회 완료 - userId={}, charmId={}, recommendationCount={}",
                userId, charmId, recommendations.size());
        return CharmRecommendationResponse.builder()
                .charm(toCharmDetailResponse(userId, charm))
                .recommendations(recommendations)
                .build();
    }

    private static String resolveCollectionName(Charm charm) {
        return Objects.requireNonNullElse(charm.getCollectionName(), UNCATEGORIZED_COLLECTION_NAME);
    }

    private CharmDetailResponse toCharmDetailResponse(Long userId, Charm charm) {
        CharmDetailResponse.CharmDetailResponseBuilder builder = CharmDetailResponse.builder()
                .id(charm.getId())
                .name(charm.getName())
                .price(charm.getPrice())
                .color(charm.getColor())
                .imgUrl(charm.getImgUrl())
                .description(charm.getDescription())
                .collectionName(charm.getCollectionName())
                .seasonLimited(charm.isSeasonLimited());

        if (charm.isSeasonLimited()) {
            boolean purchasable = storyService.isSeasonCompleted(userId, charm.getCharacterId(), charm.getSeason());
            return builder.purchasable(purchasable).build();
        }

        Character character = characterRepository.findById(charm.getCharacterId())
                .orElseThrow(() -> new CustomException(CharacterErrorCode.CHARACTER_NOT_FOUND));
        return builder.character(CharmCharacterResponse.builder()
                        .id(character.getId())
                        .name(character.getName())
                        .personality(character.getPersonality())
                        .intro(character.getIntro())
                        .build())
                .build();
    }
}
