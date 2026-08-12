package com.meisterbear.domain.story.service;

import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.story.dto.response.CurrentSeasonResponse;
import com.meisterbear.domain.story.dto.response.PastSeasonResponse;
import com.meisterbear.domain.story.dto.response.StoryListResponse;
import com.meisterbear.domain.story.dto.response.StoryProgressResponse;
import com.meisterbear.domain.story.entity.Story;
import com.meisterbear.domain.story.entity.UserStoryProgress;
import com.meisterbear.domain.story.repository.StoryRepository;
import com.meisterbear.domain.story.repository.UserStoryProgressRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private final CollectionRepository collectionRepository;
    private final StoryRepository storyRepository;
    private final UserStoryProgressRepository userStoryProgressRepository;

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
        String currentSeasonName = seasons.get(seasons.size() - 1);
        List<Story> currentSeasonStories = storiesBySeason.get(currentSeasonName);

        List<Long> storyIds = currentSeasonStories.stream().map(Story::getId).toList();
        Map<Long, UserStoryProgress> progressByStoryId = userStoryProgressRepository
                .findByUserIdAndStoryIdIn(userId, storyIds).stream()
                .collect(Collectors.toMap(UserStoryProgress::getStoryId, progress -> progress));

        List<StoryProgressResponse> storyResponses = currentSeasonStories.stream()
                .sorted((a, b) -> Integer.compare(a.getUnlockOrder(), b.getUnlockOrder()))
                .map(story -> toStoryProgressResponse(story, progressByStoryId.get(story.getId())))
                .toList();

        List<PastSeasonResponse> pastSeasons = seasons.stream()
                .filter(season -> !season.equals(currentSeasonName))
                .map(season -> toPastSeasonResponse(season, storiesBySeason.get(season)))
                .toList();

        log.info("[StoryService] 스토리 목록 조회 완료 - userId={}, currentSeason={}", userId, currentSeasonName);
        return StoryListResponse.builder()
                .currentSeason(CurrentSeasonResponse.builder()
                        .season(currentSeasonName)
                        .stories(storyResponses)
                        .build())
                .pastSeasons(pastSeasons)
                .build();
    }

    private StoryProgressResponse toStoryProgressResponse(Story story, UserStoryProgress progress) {
        return StoryProgressResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .unlockOrder(story.getUnlockOrder())
                .isLocked(story.isLocked())
                .isDone(progress != null && progress.isDone())
                .readAt(progress != null ? progress.getReadAt() : null)
                .build();
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
