package com.meisterbear.domain.charm.repository;

import com.meisterbear.domain.charm.entity.Charm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharmRepository extends JpaRepository<Charm, Long> {

    // 참 추천 - 지정한 참 하나만 빼고 전체 참 목록을 반환한다
    List<Charm> findByIdNot(Long id);

    Optional<Charm> findFirstByCharacterIdOrderByIdAsc(Long characterId);

    // 같은 캐릭터가 시즌별로 다른 참을 가질 수 있어 시즌 일치를 우선 조회
    Optional<Charm> findFirstByCharacterIdAndSeasonOrderByIdAsc(Long characterId, String season);

    // 시즌 완료 보상용 - 이 시즌에 속한 모든 캐릭터의 참
    List<Charm> findBySeasonOrderByIdAsc(String season);
}
