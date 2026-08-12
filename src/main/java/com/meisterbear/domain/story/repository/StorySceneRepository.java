package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.StoryScene;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorySceneRepository extends JpaRepository<StoryScene, Long> {

    List<StoryScene> findByStoryIdOrderBySceneOrderAsc(Long storyId);
}
