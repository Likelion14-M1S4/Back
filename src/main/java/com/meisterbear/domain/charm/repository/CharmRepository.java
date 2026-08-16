package com.meisterbear.domain.charm.repository;

import com.meisterbear.domain.charm.entity.Charm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharmRepository extends JpaRepository<Charm, Long> {

    List<Charm> findByCollectionNameAndSeasonAndIdNot(String collectionName, String season, Long id);

    // NFC 태그 캐릭터의 컬렉션명 표기용 - 캐릭터에 연결된 참에서 collection_name을 얻는다
    Optional<Charm> findFirstByCharacterId(Long characterId);
}
