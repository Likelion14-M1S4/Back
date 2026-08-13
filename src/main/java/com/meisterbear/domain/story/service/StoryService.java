package com.meisterbear.domain.story.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meisterbear.domain.character.entity.Character;
import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.repository.CharacterRepository;
import com.meisterbear.domain.character.repository.CollectionRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.story.dto.response.ChoiceResponse;
import com.meisterbear.domain.story.dto.response.StoryChoiceResultResponse;
import com.meisterbear.domain.story.dto.response.CurrentSeasonResponse;
import com.meisterbear.domain.story.dto.response.PastSeasonResponse;
import com.meisterbear.domain.story.dto.response.QuestionResponse;
import com.meisterbear.domain.story.dto.response.SceneResponse;
import com.meisterbear.domain.story.dto.response.StoryCompleteResultResponse;
import com.meisterbear.domain.story.dto.response.StoryDetailResponse;
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
        Character character = resolveCharacter(characterId);
        String productSeason = resolveProductSeason(character);
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
                        .characterName(character.getName())
                        .characterImgUrl(character.getImgUrl())
                        .stories(storyResponses)
                        .isAllCompleted(allCompleted)
                        .build())
                .pastSeasons(pastSeasons)
                .build();
    }

    public StoryDetailResponse findStoryDetail(Long userId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        if (isLocked(userId, story)) {
            throw new CustomException(StoryErrorCode.STORY_LOCKED);
        }

        UserStoryProgress progress = userStoryProgressRepository.findByUserIdAndStoryId(userId, storyId)
                .orElse(null);
        Character character = resolveCharacter(story.getCharacterId());

        log.info("[StoryService] 챕터 상세 조회 완료 - userId={}, storyId={}", userId, storyId);
        return StoryDetailResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .unlockOrder(story.getUnlockOrder())
                .isDone(progress != null && progress.isDone())
                .readAt(progress != null ? progress.getReadAt() : null)
                .scenes(parseScenes(story.getScenes()))
                .question(parseQuestion(story.getQuestions()))
                .isSeasonCompleted(isSeasonCompleted(userId, story))
                .characterName(character.getName())
                .characterImgUrl(character.getImgUrl())
                .build();
    }

    public StoryChoiceResultResponse selectChoice(Long userId, Long storyId, Long choiceId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        if (isLocked(userId, story)) {
            throw new CustomException(StoryErrorCode.STORY_LOCKED);
        }

        String tagName = resolveChoiceTagName(story.getQuestions(), choiceId);

        log.info("[StoryService] 챕터 선택 완료 - userId={}, storyId={}, choiceId={}", userId, storyId, choiceId);
        return StoryChoiceResultResponse.builder()
                .storyId(storyId)
                .tagName(tagName)
                .build();
    }

    // 스토리를 끝까지 다 본 뒤 완료 버튼을 눌렀을 때 호출 - 여기서만 챕터를 완료 처리한다
    @Transactional
    public StoryCompleteResultResponse completeStory(Long userId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        if (isLocked(userId, story)) {
            throw new CustomException(StoryErrorCode.STORY_LOCKED);
        }

        UserStoryProgress progress = userStoryProgressRepository.findByUserIdAndStoryId(userId, storyId)
                .orElseGet(() -> UserStoryProgress.builder().userId(userId).storyId(storyId).build());
        progress.complete();
        userStoryProgressRepository.save(progress);
        Character character = resolveCharacter(story.getCharacterId());

        log.info("[StoryService] 챕터 완료 처리 - userId={}, storyId={}", userId, storyId);
        return StoryCompleteResultResponse.builder()
                .storyId(storyId)
                .isDone(progress.isDone())
                .readAt(progress.getReadAt())
                .isSeasonCompleted(isSeasonCompleted(userId, story))
                .characterName(character.getName())
                .characterImgUrl(character.getImgUrl())
                .build();
    }

    // questions[0].choices 중 choiceId와 일치하는 항목의 tagName을 반환 (유효성 검증 겸함)
    private String resolveChoiceTagName(String questionsJson, Long choiceId) {
        if (questionsJson == null || questionsJson.isBlank()) {
            throw new CustomException(StoryErrorCode.INVALID_CHOICE);
        }
        try {
            JsonNode questions = objectMapper.readTree(questionsJson);
            if (!questions.isArray() || questions.isEmpty()) {
                throw new CustomException(StoryErrorCode.INVALID_CHOICE);
            }
            JsonNode question = questions.get(0);
            return StreamSupport.stream(question.path("choices").spliterator(), false)
                    .filter(choice -> choice.path("id").asLong() == choiceId)
                    .findFirst()
                    .map(choice -> choice.path("tagName").asText(null))
                    .orElseThrow(() -> new CustomException(StoryErrorCode.INVALID_CHOICE));
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[StoryService] questions JSON 파싱 실패 - {}", e.getMessage());
            throw new CustomException(StoryErrorCode.INVALID_CHOICE);
        }
    }

    // 1번 챕터는 항상 해금, 그 외는 직전 챕터를 이 유저가 완주했는지로 판단
    private boolean isLocked(Long userId, Story story) {
        if (story.getUnlockOrder() == 1) {
            return false;
        }
        return storyRepository
                .findByCharacterIdAndSeasonAndUnlockOrder(story.getCharacterId(), story.getSeason(),
                        story.getUnlockOrder() - 1)
                .map(previous -> userStoryProgressRepository.findByUserIdAndStoryId(userId, previous.getId())
                        .map(p -> !p.isDone())
                        .orElse(true))
                // 직전 챕터 데이터 자체가 없는 건 정합성 문제이므로 안전하게 잠금 처리
                .orElse(true);
    }

    private List<SceneResponse> parseScenes(String scenesJson) {
        if (scenesJson == null || scenesJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode scenes = objectMapper.readTree(scenesJson);
            if (!scenes.isArray()) {
                return List.of();
            }
            return StreamSupport.stream(scenes.spliterator(), false)
                    .map(node -> SceneResponse.builder()
                            .order(node.path("order").asInt())
                            .imgUrl(node.path("imgUrl").asText(null))
                            .content(node.path("content").asText(null))
                            .build())
                    .sorted(Comparator.comparing(SceneResponse::getOrder))
                    .toList();
        } catch (Exception e) {
            log.warn("[StoryService] scenes JSON 파싱 실패 - {}", e.getMessage());
            return List.of();
        }
    }

    // 챕터당 질문은 1개만 사용 (questions 배열의 첫 번째 요소)
    private QuestionResponse parseQuestion(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode questions = objectMapper.readTree(questionsJson);
            if (!questions.isArray() || questions.isEmpty()) {
                return null;
            }
            JsonNode question = questions.get(0);
            List<ChoiceResponse> choices = StreamSupport.stream(question.path("choices").spliterator(), false)
                    .map(choice -> ChoiceResponse.builder()
                            .id(choice.path("id").asLong())
                            .label(choice.path("label").asText(null))
                            .build())
                    .toList();
            return QuestionResponse.builder()
                    .question(question.path("question").asText(null))
                    .choices(choices)
                    .build();
        } catch (Exception e) {
            log.warn("[StoryService] questions JSON 파싱 실패 - {}", e.getMessage());
            return null;
        }
    }

    private Character resolveCharacter(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.CHARACTER_NOT_FOUND));
    }

    // 유저가 등록한 제품(캐릭터의 원본 제품) 자체의 season 값을 그대로 사용한다.
    private String resolveProductSeason(Character character) {
        Product product = productRepository.findById(character.getProductId())
                .orElseThrow(() -> new CustomException(StoryErrorCode.PRODUCT_NOT_FOUND));
        return product.getSeason();
    }

    // 이 챕터가 속한 시즌의 모든 챕터를 이 유저가 완주했는지 (시즌 완료 화면 노출 여부)
    private boolean isSeasonCompleted(Long userId, Story story) {
        List<Story> seasonStories = storyRepository.findByCharacterIdAndSeason(story.getCharacterId(), story.getSeason());
        List<Long> seasonStoryIds = seasonStories.stream().map(Story::getId).toList();
        Map<Long, UserStoryProgress> progressByStoryId = userStoryProgressRepository
                .findByUserIdAndStoryIdIn(userId, seasonStoryIds).stream()
                .collect(Collectors.toMap(UserStoryProgress::getStoryId, progress -> progress));
        return seasonStories.stream()
                .allMatch(s -> {
                    UserStoryProgress progress = progressByStoryId.get(s.getId());
                    return progress != null && progress.isDone();
                });
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
                .thumbnailUrl(story.getThumbnailUrl())
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
