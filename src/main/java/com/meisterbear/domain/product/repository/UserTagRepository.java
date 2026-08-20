package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.TagType;
import com.meisterbear.domain.product.entity.UserTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    void deleteByUserId(Long userId);

    List<UserTag> findByUserIdAndTagTypeOrderByTaggedAtDesc(Long userId, TagType tagType);

    List<UserTag> findByUserIdAndStoreIdAndTagTypeOrderByTaggedAtDesc(Long userId, Long storeId, TagType tagType);

    // 하루 1회 중복 방지용 - 오늘 자정 이후 존재 여부만 확인
    boolean existsByUserIdAndProductIdAndStoreIdAndTagTypeAndTaggedAtGreaterThanEqual(
            Long userId, Long productId, Long storeId, TagType tagType, java.time.LocalDateTime start);
}
