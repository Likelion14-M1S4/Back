package com.meisterbear.domain.character.repository;

import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.entity.CollectionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Optional<Collection> findTopByUserIdOrderByAddedAtDesc(Long userId);

    boolean existsByUserIdAndCharacterIdAndStatus(Long userId, Long characterId, CollectionStatus status);
}
