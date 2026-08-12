package com.meisterbear.domain.story.service;

import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.story.dto.request.SelectStoryChoiceRequest;
import com.meisterbear.domain.story.dto.response.CurrentSeasonResponse;
import com.meisterbear.domain.story.dto.response.PastSeasonResponse;
import com.meisterbear.domain.story.dto.response.StoryChoiceResponse;
import com.meisterbear.domain.story.dto.response.StoryChoiceSelectResponse;
import com.meisterbear.domain.story.dto.response.StoryDetailResponse;
import com.meisterbear.domain.story.dto.response.StoryListResponse;
import com.meisterbear.domain.story.dto.response.StoryProgressResponse;
import com.meisterbear.domain.story.dto.response.StoryQuestionResponse;
import com.meisterbear.domain.story.dto.response.StorySceneResponse;
import com.meisterbear.domain.story.entity.Story;
import com.meisterbear.domain.story.entity.StoryChoice;
import com.meisterbear.domain.story.entity.StoryQuestion;
import com.meisterbear.domain.story.entity.StoryScene;
import com.meisterbear.domain.story.entity.UserChoice;
import com.meisterbear.domain.story.entity.UserStoryProgress;
import com.meisterbear.domain.story.exception.StoryErrorCode;
import com.meisterbear.domain.story.repository.StoryChoiceRepository;
import com.meisterbear.domain.story.repository.StoryQuestionRepository;
import com.meisterbear.domain.story.repository.StoryRepository;
import com.meisterbear.domain.story.repository.StorySceneRepository;
import com.meisterbear.domain.story.repository.UserChoiceRepository;
import com.meisterbear.domain.story.repository.UserStoryProgressRepository;
import com.meisterbear.global.exception.CustomException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    // 시즌 코드 규칙: SS{연도}(봄·여름) / AW{연도}(가을·겨울) 예) SS2026, AW2026
    private static final Pattern SEASON_PATTERN = Pattern.compile("^(SS|AW)(\\d{4})$");

    private final CollectionRepository collectionRepository;
    private final StoryRepository storyRepository;
    private final StorySceneRepository storySceneRepository;
    private final StoryQuestionRepository storyQuestionRepository;
    private final StoryChoiceRepository storyChoiceRepository;
    private final UserStoryProgressRepository userStoryProgressRepository;
    private final UserChoiceRepository userChoiceRepository;

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
        String currentSeasonName = resolveCurrentSeason(seasons);
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

    // 오늘이 포함된 시즌을 current로 판단. 오늘이 포함된 시즌이 없으면 이미 시작된 시즌 중 가장 최근 것,
    // 전부 미래 시즌뿐이면 가장 이른 시즌으로 폴백한다.
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
        return "SS".equals(matcher.group(1))
                ? LocalDate.of(year, 8, 31)
                : LocalDate.of(year + 1, 2, 28);
    }

    public StoryDetailResponse findStoryDetail(Long userId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));
        if (story.isLocked()) {
            throw new CustomException(StoryErrorCode.STORY_LOCKED);
        }

        List<StoryScene> scenes = storySceneRepository.findByStoryIdOrderBySceneOrderAsc(storyId);
        List<StoryQuestion> questions = storyQuestionRepository.findByStoryId(storyId);
        List<Long> questionIds = questions.stream().map(StoryQuestion::getId).toList();
        Map<Long, List<StoryChoice>> choicesByQuestionId = storyChoiceRepository.findByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.groupingBy(StoryChoice::getQuestionId));

        boolean isDone = userStoryProgressRepository.findByUserIdAndStoryId(userId, storyId)
                .map(UserStoryProgress::isDone)
                .orElse(false);

        log.info("[StoryService] 스토리 상세 조회 완료 - userId={}, storyId={}", userId, storyId);
        return StoryDetailResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .unlockOrder(story.getUnlockOrder())
                .isLocked(story.isLocked())
                .isDone(isDone)
                .scenes(scenes.stream().map(this::toSceneResponse).toList())
                .questions(questions.stream()
                        .map(question -> toQuestionResponse(question,
                                choicesByQuestionId.getOrDefault(question.getId(), List.of())))
                        .toList())
                .build();
    }

    @Transactional
    public StoryChoiceSelectResponse selectChoice(Long userId, Long storyId, SelectStoryChoiceRequest request) {
        StoryQuestion question = storyQuestionRepository.findById(request.getQuestionId())
                .filter(q -> q.getStoryId().equals(storyId))
                .orElseThrow(() -> new CustomException(StoryErrorCode.INVALID_CHOICE));

        StoryChoice choice = storyChoiceRepository.findById(request.getChoiceId())
                .filter(c -> c.getQuestionId().equals(question.getId()))
                .orElseThrow(() -> new CustomException(StoryErrorCode.INVALID_CHOICE));

        UserChoice userChoice = userChoiceRepository.save(
                UserChoice.builder()
                        .userId(userId)
                        .choiceId(choice.getId())
                        .build());

        log.info("[StoryService] 선택지 저장 완료 - userId={}, storyId={}, choiceId={}", userId, storyId, choice.getId());
        return StoryChoiceSelectResponse.builder()
                .userChoiceId(userChoice.getId())
                .tagName(choice.getTagName())
                .build();
    }

    private StorySceneResponse toSceneResponse(StoryScene scene) {
        return StorySceneResponse.builder()
                .id(scene.getId())
                .sceneOrder(scene.getSceneOrder())
                .imgUrl(scene.getImgUrl())
                .content(scene.getContent())
                .build();
    }

    private StoryQuestionResponse toQuestionResponse(StoryQuestion question, List<StoryChoice> choices) {
        return StoryQuestionResponse.builder()
                .id(question.getId())
                .question(question.getQuestion())
                .choices(choices.stream().map(this::toChoiceResponse).toList())
                .build();
    }

    private StoryChoiceResponse toChoiceResponse(StoryChoice choice) {
        return StoryChoiceResponse.builder()
                .id(choice.getId())
                .label(choice.getLabel())
                .tagName(choice.getTagName())
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
