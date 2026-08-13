package com.meisterbear.domain.story.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.story.dto.response.CurrentSeasonResponse;
import com.meisterbear.domain.story.dto.response.PastSeasonResponse;
import com.meisterbear.domain.story.dto.response.StoryListResponse;
import com.meisterbear.domain.story.dto.response.StoryProgressResponse;
import com.meisterbear.domain.story.entity.Story;
import com.meisterbear.domain.story.entity.UserStoryProgress;
import com.meisterbear.domain.story.exception.StoryErrorCode;
import com.meisterbear.domain.story.repository.StoryRepository;
import com.meisterbear.domain.story.repository.UserStoryProgressRepository;
import com.meisterbear.global.exception.CustomException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    // 시즌 코드 규칙: SS{연도}(봄·여름) / AW{연도}(가을·겨울) 예) SS2026, AW2026
    private static final Pattern SEASON_PATTERN = Pattern.compile("^(SS|AW)(\\d{4})$");

    private final CollectionRepository collectionRepository;
    private final CharacterRepository characterRepository;
    private final ProductRepository productRepository;
    private final StoryRepository storyRepository;
    private final UserStoryProgressRepository userStoryProgressRepository;
    private final ObjectMapper objectMapper;

    public StoryListResponse findStories(Long userId) {
        Optional<Collection> latestCollection = collectionRepository.findTopByUserIdOrderByAddedAtDesc(userId);
        if (latestCollection.isEmpty()) {
            log.info("[StoryService] 스토리 목록 조회 완료(등록 제품 없음) - userId={}", userId);
            return StoryListResponse.empty();
        }

        Long characterId = latestCollection.get().getCharacterId();
        List<Story> stories = storyRepository.findByCharacterIdOrderByIdAsc(characterId);
        if (stories.isEmpty()) {
            log.info("[StoryService] 스토리 목록 조회 완료(스토리 없음) - userId={}", userId);
            return StoryListResponse.empty();
        }

        Map<String, List<Story>> storiesBySeason = stories.stream()
                .collect(Collectors.groupingBy(Story::getSeason, LinkedHashMap::new, Collectors.toList()));

        List<String> seasons = List.copyOf(storiesBySeason.keySet());
        String productSeason = resolveProductSeason(characterId);
        String currentSeasonName = storiesBySeason.containsKey(productSeason)
                ? productSeason
                : resolveCurrentSeason(seasons);
        List<Story> currentSeasonStories = storiesBySeason.get(currentSeasonName);

        List<Long> storyIds = currentSeasonStories.stream().map(Story::getId).toList();
        Map<Long, UserStoryProgress> progressByStoryId = userStoryProgressRepository
                .findByUserIdAndStoryIdIn(userId, storyIds).stream()
                .collect(Collectors.toMap(UserStoryProgress::getStoryId, progress -> progress));

        List<Story> sortedStories = currentSeasonStories.stream()
                .sorted(Comparator.comparing(Story::getUnlockOrder))
                .toList();
        List<StoryProgressResponse> storyResponses = new ArrayList<>();
        for (int i = 0; i < sortedStories.size(); i++) {
            Story story = sortedStories.get(i);
            UserStoryProgress progress = progressByStoryId.get(story.getId());
            // 1번 챕터는 항상 해금, 그 외는 직전 챕터를 이 유저가 완주했는지로 판단 (전역 상태 아님)
            boolean locked;
            if (i == 0) {
                locked = false;
            } else {
                UserStoryProgress previousProgress = progressByStoryId.get(sortedStories.get(i - 1).getId());
                locked = previousProgress == null || !previousProgress.isDone();
            }
            storyResponses.add(toStoryProgressResponse(story, progress, locked));
        }

        boolean allCompleted = sortedStories.stream()
                .allMatch(story -> {
                    UserStoryProgress progress = progressByStoryId.get(story.getId());
                    return progress != null && progress.isDone();
                });

        List<PastSeasonResponse> pastSeasons = seasons.stream()
                .filter(season -> !season.equals(currentSeasonName))
                .map(season -> toPastSeasonResponse(season, storiesBySeason.get(season)))
                .toList();

        log.info("[StoryService] 스토리 목록 조회 완료 - userId={}, currentSeason={}", userId, currentSeasonName);
        return StoryListResponse.builder()
                .currentSeason(CurrentSeasonResponse.builder()
                        .season(currentSeasonName)
                        .stories(storyResponses)
                        .isAllCompleted(allCompleted)
                        .build())
                .pastSeasons(pastSeasons)
                .build();
    }

    // 유저가 등록한 제품(캐릭터의 원본 제품) 자체의 season 값을 그대로 사용한다.
    private String resolveProductSeason(Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.CHARACTER_NOT_FOUND));
        Product product = productRepository.findById(character.getProductId())
                .orElseThrow(() -> new CustomException(StoryErrorCode.PRODUCT_NOT_FOUND));
        return product.getSeason();
    }

    // 오늘이 포함된 시즌을 current로 판단. 없으면 이미 시작된 시즌 중 가장 최근 것, 그것도 없으면 가장 이른 시즌으로 폴백한다.
    private String resolveCurrentSeason(List<String> seasons) {
        LocalDate today = LocalDate.now();
        return seasons.stream()
                .filter(season -> {
                    LocalDate start = seasonStartDate(season);
                    LocalDate end = seasonEndDate(season);
                    return !today.isBefore(start) && !today.isAfter(end);
                })
                .findFirst()
                .orElseGet(() -> seasons.stream()
                        .filter(season -> !seasonStartDate(season).isAfter(today))
                        .max(Comparator.comparing(this::seasonStartDate))
                        .orElseGet(() -> seasons.stream()
                                .min(Comparator.comparing(this::seasonStartDate))
                                .orElse(seasons.get(seasons.size() - 1))));
    }

    private LocalDate seasonStartDate(String season) {
        Matcher matcher = SEASON_PATTERN.matcher(season);
        if (!matcher.matches()) {
            return LocalDate.MIN;
        }
        int year = Integer.parseInt(matcher.group(2));
        return "SS".equals(matcher.group(1)) ? LocalDate.of(year, 3, 1) : LocalDate.of(year, 9, 1);
    }

    private LocalDate seasonEndDate(String season) {
        Matcher matcher = SEASON_PATTERN.matcher(season);
        if (!matcher.matches()) {
            return LocalDate.MIN;
        }
        int year = Integer.parseInt(matcher.group(2));
        return "SS".equals(matcher.group(1)) ? LocalDate.of(year, 8, 31) : LocalDate.of(year + 1, 2, 28);
    }

    private StoryProgressResponse toStoryProgressResponse(Story story, UserStoryProgress progress, boolean locked) {
        boolean done = progress != null && progress.isDone();
        return StoryProgressResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .unlockOrder(story.getUnlockOrder())
                .isLocked(locked)
                .isDone(done)
                .readAt(progress != null ? progress.getReadAt() : null)
                .teaser(done ? null : extractFirstSceneContent(story.getScenes()))
                .build();
    }

    // 미완주 챕터 카드에 보여줄 짧은 소개 문구를 story.scenes JSON의 첫 장면에서 추출한다.
    private String extractFirstSceneContent(String scenesJson) {
        if (scenesJson == null || scenesJson.isBlank()) {
            return null;
        }
        try {
            JsonNode scenes = objectMapper.readTree(scenesJson);
            if (!scenes.isArray() || scenes.isEmpty()) {
                return null;
            }
            JsonNode firstScene = StreamSupport.stream(scenes.spliterator(), false)
                    .min(Comparator.comparingInt(node -> node.path("order").asInt(Integer.MAX_VALUE)))
                    .orElse(null);
            return firstScene != null ? firstScene.path("content").asText(null) : null;
        } catch (Exception e) {
            log.warn("[StoryService] scenes JSON 파싱 실패 - {}", e.getMessage());
            return null;
        }
    }

    private PastSeasonResponse toPastSeasonResponse(String season, List<Story> seasonStories) {
        String thumbnailUrl = seasonStories.stream()
                .filter(story -> story.getUnlockOrder() == 1)
                .map(Story::getThumbnailUrl)
                .findFirst()
                .orElse(seasonStories.get(0).getThumbnailUrl());
        return PastSeasonResponse.builder()
                .season(season)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }
}
