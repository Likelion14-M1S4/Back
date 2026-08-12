package com.meisterbear.domain.story.repository;

import com.meisterbear.domain.story.entity.UserChoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChoiceRepository extends JpaRepository<UserChoice, Long> {
}
