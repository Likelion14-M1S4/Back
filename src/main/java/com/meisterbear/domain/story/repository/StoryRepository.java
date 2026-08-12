package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.Story;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByCharacterIdOrderByIdAsc(Long characterId);
}
