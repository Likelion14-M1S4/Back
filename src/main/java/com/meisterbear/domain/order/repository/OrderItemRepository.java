package com.meisterbear.domain.order.repository;

import com.meisterbear.domain.order.entity.OrderItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    void deleteByUserId(Long userId);

    // 제품 상세의 isPurchased 판단용
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // 등록 제품 목록 조회용. 화면 정렬은 서비스에서 등록일(수령/구매) 기준으로 수행하므로 여기선 정렬하지 않는다
    List<OrderItem> findByUserId(Long userId);

    // 등록 제품 상세 - 본인 구매 기록만 조회되도록 userId를 함께 대조
    Optional<OrderItem> findByIdAndUserId(Long id, Long userId);

    // 정품 인증서 - NFC uid로 특정된 제품의 내 구매 기록
    Optional<OrderItem> findFirstByUserIdAndProductIdOrderByOrderedAtDescIdDesc(Long userId, Long productId);

    // 정품 인증서 - uid 없이 호출 시 최근 구매 1건 기준
    Optional<OrderItem> findFirstByUserIdOrderByOrderedAtDescIdDesc(Long userId);
}
