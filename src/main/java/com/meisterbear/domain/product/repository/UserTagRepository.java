package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    void deleteByUserId(Long userId);
}
