package com.meisterbear.domain.charm.repository;

import com.meisterbear.domain.charm.entity.Charm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharmRepository extends JpaRepository<Charm, Long> {
}
