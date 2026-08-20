package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.Story;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByCharacterIdOrderByIdAsc(Long characterId);

    // 스토리는 유저가 태그한 캐릭터와 무관하게 전부에게 동일하게 노출한다(시즌 하나만 운영) -
    // 그 기준이 될 시즌 스토리 하나를 정하는 데 사용
    Optional<Story> findFirstByOrderByIdAsc();

    List<Story> findByCharacterIdAndSeason(Long characterId, String season);

    Optional<Story> findByCharacterIdAndSeasonAndUnlockOrder(Long characterId, String season, Integer unlockOrder);
}
