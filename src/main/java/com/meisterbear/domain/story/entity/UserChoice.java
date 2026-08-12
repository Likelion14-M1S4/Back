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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Table(name = "user_choice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "choice_id", nullable = false)
    private Long choiceId;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    @Builder
    private UserChoice(Long userId, Long choiceId) {
        this.userId = userId;
        this.choiceId = choiceId;
    }
}