package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.StoryQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryQuestionRepository extends JpaRepository<StoryQuestion, Long> {

    List<StoryQuestion> findByStoryId(Long storyId);
}
