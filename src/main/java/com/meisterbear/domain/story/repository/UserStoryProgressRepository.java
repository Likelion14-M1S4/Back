package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.UserStoryProgress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStoryProgressRepository extends JpaRepository<UserStoryProgress, Long> {

    List<UserStoryProgress> findByUserIdAndStoryIdIn(Long userId, List<Long> storyIds);
}
