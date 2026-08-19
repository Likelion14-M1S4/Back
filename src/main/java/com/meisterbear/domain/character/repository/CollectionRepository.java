package com.meisterbear.domain.character.repository;

import com.meisterbear.domain.character.entity.Collection;
import com.meisterbear.domain.character.entity.CollectionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Optional<Collection> findTopByUserIdOrderByAddedAtDesc(Long userId);

    boolean existsByUserIdAndCharacterIdAndStatus(Long userId, Long characterId, CollectionStatus status);

    // 참 목록 조회 시 "uid로 추가(collect)한 캐릭터"만 필터링하는 데 사용 - NFC로 소유 확인된 캐릭터만 포함
    @Query("SELECT c.characterId FROM Collection c WHERE c.userId = :userId AND c.status = :status")
    List<Long> findCharacterIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CollectionStatus status);

    // NFC 태그 캐릭터 수집 - 기존 수집 행 유무/상태 확인용.
    // 스키마에 (user_id, character_id) 유니크 제약이 없어 중복 행이 존재할 수 있으므로,
    // 단건 강제(Optional) 대신 List로 받아 NonUniqueResult 예외를 원천 차단한다
    List<Collection> findByUserIdAndCharacterId(Long userId, Long characterId);

    void deleteByUserId(Long userId);
}
