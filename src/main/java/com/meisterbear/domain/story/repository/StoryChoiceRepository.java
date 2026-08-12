package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.StoryChoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryChoiceRepository extends JpaRepository<StoryChoice, Long> {

    List<StoryChoice> findByQuestionIdIn(List<Long> questionIds);
}
