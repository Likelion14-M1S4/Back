package com.meisterbear.domain.story.service;

import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.story.dto.request.SelectStoryChoiceRequest;
import com.meisterbear.domain.story.dto.response.CurrentSeasonResponse;
import com.meisterbear.domain.story.dto.response.PastSeasonResponse;
import com.meisterbear.domain.story.dto.response.StoryChoiceResponse;
import com.meisterbear.domain.story.dto.response.StoryChoiceSelectResponse;
import com.meisterbear.domain.story.dto.response.StoryCompleteResponse;
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
import java.util.ArrayList;
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
    private final CharacterRepository characterRepository;
    private final ProductRepository productRepository;
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
        String productSeason = resolveProductSeason(characterId);
        // 유저가 등록한 제품의 시즌을 우선 사용하고, 그 시즌 스토리가 아직 없으면(콘텐츠 미시딩 등) 날짜 기반으로 폴백한다.
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
            // 1번 챕터는 항상 해금, 그 외는 직전 챕터를 이 유저가 완주했는지로 판단 (전역 상태 아님)
            boolean locked;
            if (i == 0) {
                locked = false;
            } else {
                UserStoryProgress previousProgress = progressByStoryId.get(sortedStories.get(i - 1).getId());
                locked = previousProgress == null || !previousProgress.isDone();
            }
            storyResponses.add(toStoryProgressResponse(story, progressByStoryId.get(story.getId()), locked));
        }

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

    // 유저가 등록한 제품(캐릭터의 원본 제품) 자체의 season 값을 그대로 사용한다.
    private String resolveProductSeason(Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalStateException("캐릭터를 찾을 수 없습니다. characterId=" + characterId));
        Product product = productRepository.findById(character.getProductId())
                .orElseThrow(() -> new IllegalStateException("제품을 찾을 수 없습니다. productId=" + character.getProductId()));
        return product.getSeason();
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
        boolean unlocked = isUnlockedForUser(userId, story);
        if (!unlocked) {
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
                .isLocked(!unlocked)
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
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));
        if (!isUnlockedForUser(userId, story)) {
            throw new CustomException(StoryErrorCode.STORY_LOCKED);
        }

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

    @Transactional
    public StoryCompleteResponse completeStory(Long userId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));
        if (!isUnlockedForUser(userId, story)) {
            throw new CustomException(StoryErrorCode.STORY_LOCKED);
        }

        UserStoryProgress progress = userStoryProgressRepository.findByUserIdAndStoryId(userId, storyId)
                .orElseGet(() -> UserStoryProgress.builder().userId(userId).storyId(storyId).build());
        if (progress.isDone()) {
            throw new CustomException(StoryErrorCode.ALREADY_COMPLETED);
        }
        progress.complete();
        userStoryProgressRepository.save(progress);

        Long characterId = story.getCharacterId();
        String season = story.getSeason();

        // 다음 챕터는 전역 상태를 바꾸지 않는다 - 해금 여부는 조회 시점에 유저별로 계산된다(isUnlockedForUser)
        Optional<Story> nextStory = storyRepository.findByCharacterIdAndSeasonAndUnlockOrder(
                characterId, season, story.getUnlockOrder() + 1);

        boolean isAllCompleted = isSeasonCompleted(userId, characterId, season);

        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalStateException("캐릭터를 찾을 수 없습니다. characterId=" + characterId));

        log.info("[StoryService] 챕터 완주 처리 완료 - userId={}, storyId={}, isAllCompleted={}",
                userId, storyId, isAllCompleted);
        return StoryCompleteResponse.builder()
                .isDone(true)
                .isAllCompleted(isAllCompleted)
                .nextStoryId(nextStory.map(Story::getId).orElse(null))
                .productId(character.getProductId())
                .build();
    }

    // 1번 챕터는 항상 해금, 그 외는 "이 유저가" 직전 챕터를 완주했는지로 판단한다.
    // Story.is_locked 전역 컬럼은 여러 유저가 캐릭터를 공유하므로 해금 판정에 쓰지 않는다.
    private boolean isUnlockedForUser(Long userId, Story story) {
        if (story.getUnlockOrder() <= 1) {
            return true;
        }
        return storyRepository.findByCharacterIdAndSeasonAndUnlockOrder(
                        story.getCharacterId(), story.getSeason(), story.getUnlockOrder() - 1)
                .map(previous -> userStoryProgressRepository.findByUserIdAndStoryId(userId, previous.getId())
                        .map(UserStoryProgress::isDone)
                        .orElse(false))
                .orElse(false);
    }

    private boolean isSeasonCompleted(Long userId, Long characterId, String season) {
        List<Story> seasonStories = storyRepository.findByCharacterIdAndSeason(characterId, season);
        List<Long> storyIds = seasonStories.stream().map(Story::getId).toList();
        Map<Long, UserStoryProgress> progressByStoryId = userStoryProgressRepository
                .findByUserIdAndStoryIdIn(userId, storyIds).stream()
                .collect(Collectors.toMap(UserStoryProgress::getStoryId, progress -> progress));
        return seasonStories.stream()
                .allMatch(s -> {
                    UserStoryProgress p = progressByStoryId.get(s.getId());
                    return p != null && p.isDone();
                });
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

    private StoryProgressResponse toStoryProgressResponse(Story story, UserStoryProgress progress, boolean isLocked) {
        return StoryProgressResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .unlockOrder(story.getUnlockOrder())
                .isLocked(isLocked)
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
