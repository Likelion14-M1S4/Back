package com.meisterbear.domain.charm.repository;

import com.meisterbear.domain.charm.entity.Charm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharmRepository extends JpaRepository<Charm, Long> {

    // 참 추천 - 지정한 참 하나만 빼고 전체 참 목록을 반환한다
    List<Charm> findByIdNot(Long id);

    // NFC 태그 캐릭터의 컬렉션명 표기용 - 캐릭터에 연결된 참에서 collection_name을 얻는다.
    // 복수 참 연결 시에도 결정적으로 같은 값이 나오도록 정렬을 명시한다
    Optional<Charm> findFirstByCharacterIdOrderByIdAsc(Long characterId);

    // 같은 캐릭터가 시즌별로 다른 참(다른 컬렉션명)을 가질 수 있으므로,
    // 태그한 제품의 시즌과 일치하는 참을 우선 조회한다
    Optional<Charm> findFirstByCharacterIdAndSeasonOrderByIdAsc(Long characterId, String season);

    // 시즌 완료 보상으로 내려줄 참 전체 목록 - 캐릭터 1개당 참 1개라, 이 시즌에 속한 모든 캐릭터의 참을 다 가져온다
    List<Charm> findBySeasonOrderByIdAsc(String season);
}
