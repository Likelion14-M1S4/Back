package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.ProductStore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStoreRepository extends JpaRepository<ProductStore, Long> {

    // NFC 태그 시 방문 매장 기록용 - 제품이 진열된 매장.
    // 시연 데이터는 제품당 1곳이지만, 복수 연결 시에도 항상 같은 매장이 선택되도록 정렬을 명시한다
    Optional<ProductStore> findFirstByProductIdOrderByStoreIdAsc(Long productId);
}
