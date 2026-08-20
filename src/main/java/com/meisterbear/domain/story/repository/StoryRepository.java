package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.Story;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByCharacterIdOrderByIdAsc(Long characterId);

    // 시즌 하나만 운영 - 기준 시즌을 정하는 데 사용
    Optional<Story> findFirstByOrderByIdAsc();

    List<Story> findByCharacterIdAndSeason(Long characterId, String season);

    Optional<Story> findByCharacterIdAndSeasonAndUnlockOrder(Long characterId, String season, Integer unlockOrder);
}
