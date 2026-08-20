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

    // NFC 재태그 시 같은 날 중복 기록 방지용 (태그 이력은 유저·제품·매장당 하루 1회).
    // taggedAt은 생성 시각이라 미래 값이 없으므로 "오늘 자정 이후 존재 여부"만 보면 충분하다
    // (상한 경계를 두면 DATETIME 정밀도/반올림과 상호작용하는 엣지가 생겨 일부러 두지 않음)
    boolean existsByUserIdAndProductIdAndStoreIdAndTagTypeAndTaggedAtGreaterThanEqual(
            Long userId, Long productId, Long storeId, TagType tagType, java.time.LocalDateTime start);
}
