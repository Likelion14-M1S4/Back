package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 같은 디자인의 색상/사이즈 형제 제품들 (제품 상세의 colors/sizes 구성용)
    List<Product> findByProductGroupIdOrderByIdAsc(Long productGroupId);
}
