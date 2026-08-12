package com.meisterbear.domain.story.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "story_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Builder
    private StoryQuestion(String question, Long storyId) {
        this.question = question;
        this.storyId = storyId;
    }
}