package com.meisterbear.domain.character.repository;

import com.meisterbear.domain.character.entity.Character;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    // NFC 태그한 제품의 캐릭터 조회용 (제품:캐릭터 = 1:1)
    Optional<Character> findByProductId(Long productId);
}
