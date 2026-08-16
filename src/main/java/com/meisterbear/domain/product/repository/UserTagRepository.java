package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.TagType;
import com.meisterbear.domain.product.entity.UserTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTagRepository extends JpaRepository<UserTag, Long> {

    void deleteByUserId(Long userId);

    // 매장 태그 이력 목록 - 매장별 그룹핑 전 원본 (최근 태그 순)
    List<UserTag> findByUserIdAndTagTypeOrderByTaggedAtDesc(Long userId, TagType tagType);

    // 매장 태그 상세 - 특정 매장에서의 태그들 (날짜별 그룹핑용, 최근 순)
    List<UserTag> findByUserIdAndStoreIdAndTagTypeOrderByTaggedAtDesc(Long userId, Long storeId, TagType tagType);
}
