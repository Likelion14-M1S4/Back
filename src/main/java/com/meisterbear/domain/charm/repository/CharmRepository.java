package com.meisterbear.domain.charm.repository;

import com.meisterbear.domain.charm.entity.Charm;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharmRepository extends JpaRepository<Charm, Long> {

    List<Charm> findByCollectionNameAndSeasonAndIdNot(String collectionName, String season, Long id);
}
