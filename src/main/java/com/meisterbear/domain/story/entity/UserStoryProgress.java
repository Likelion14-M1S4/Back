package com.meisterbear.domain.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "user_story_progress")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStoryProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private UserStoryProgress(Long userId, Long storyId) {
        this.userId = userId;
        this.storyId = storyId;
        this.done = false;
    }

    // 챕터 마지막 장면 완주 처리 - 자격은 한 번만 발생하도록 중복 호출에도 안전
    public void complete() {
        if (this.done) {
            return;
        }
        this.done = true;
        this.readAt = LocalDateTime.now();
    }
}
