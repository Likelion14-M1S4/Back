package com.meisterbear.domain.order.repository;

import com.meisterbear.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    void deleteByUserId(Long userId);

    // 제품 상세의 isPurchased 판단용
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
