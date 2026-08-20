package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.ProductStore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStoreRepository extends JpaRepository<ProductStore, Long> {

    Optional<ProductStore> findFirstByProductIdOrderByStoreIdAsc(Long productId);
}
