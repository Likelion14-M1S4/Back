package com.meisterbear.domain.character.repository;

import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.entity.CollectionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Optional<Collection> findTopByUserIdOrderByAddedAtDesc(Long userId);

    boolean existsByUserIdAndCharacterIdAndStatus(Long userId, Long characterId, CollectionStatus status);

    // NFC 태그 캐릭터 수집 - 기존 수집 행 유무/상태 확인용
    Optional<Collection> findByUserIdAndCharacterId(Long userId, Long characterId);

    void deleteByUserId(Long userId);
}
