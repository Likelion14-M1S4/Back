package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 같은 디자인의 색상/사이즈 형제 제품들 (제품 상세의 colors/sizes 구성용)
    List<Product> findByProductGroupIdOrderByIdAsc(Long productGroupId);

    // 시즌 제품 목록 (season 컬럼 값은 시드 기준 "2026-FALL" 형식)
    List<Product> findBySeasonOrderByIdAsc(String season);

    // NFC 태그 검증 - 제품에 부착된 NFC uid로 조회
    Optional<Product> findByNfcUid(String nfcUid);
}
