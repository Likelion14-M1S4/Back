package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.ProductStore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStoreRepository extends JpaRepository<ProductStore, Long> {

    // NFC 태그 시 방문 매장 기록용 - 제품이 진열된 매장 (시연 데이터에선 제품당 1곳)
    Optional<ProductStore> findFirstByProductId(Long productId);
}
