package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.Story;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByCharacterIdOrderByIdAsc(Long characterId);

    // 시즌 하나만 운영 - 여기서 뽑은 기준 story의 characterId, season을
    // StoryService.findStories/isCurrentSeasonCompleted가 그대로 사용
    Optional<Story> findFirstByOrderByIdAsc();

    List<Story> findByCharacterIdAndSeason(Long characterId, String season);

    Optional<Story> findByCharacterIdAndSeasonAndUnlockOrder(Long characterId, String season, Integer unlockOrder);
}
