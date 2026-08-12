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
@Table(name = "story_choice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Builder
    private StoryChoice(Long questionId, String label, String tagName) {
        this.questionId = questionId;
        this.label = label;
        this.tagName = tagName;
    }
}