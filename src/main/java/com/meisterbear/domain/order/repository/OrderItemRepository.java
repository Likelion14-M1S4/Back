package com.meisterbear.domain.order.repository;

import com.meisterbear.domain.order.entity.OrderItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    void deleteByUserId(Long userId);

    // 제품 상세의 isPurchased 판단용
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // 등록 제품 목록 - 최근 구매 순
    List<OrderItem> findByUserIdOrderByOrderedAtDesc(Long userId);

    // 등록 제품 상세 - 본인 구매 기록만 조회되도록 userId를 함께 대조
    Optional<OrderItem> findByIdAndUserId(Long id, Long userId);
}
