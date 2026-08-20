package com.meisterbear.domain.order.repository;

import com.meisterbear.domain.order.entity.OrderItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    void deleteByUserId(Long userId);

    // 제품 상세의 isPurchased 판단용
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    List<OrderItem> findByUserId(Long userId);

    // 본인 구매 기록만 조회
    Optional<OrderItem> findByIdAndUserId(Long id, Long userId);

    Optional<OrderItem> findFirstByUserIdAndProductIdOrderByOrderedAtDescIdDesc(Long userId, Long productId);

    // 유저 무관, 이 실물의 최신 구매 기록
    Optional<OrderItem> findFirstByProductIdOrderByOrderedAtDescIdDesc(Long productId);

    Optional<OrderItem> findFirstByUserIdOrderByOrderedAtDescIdDesc(Long userId);
}
