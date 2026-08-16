package com.meisterbear.domain.product.repository;

import com.meisterbear.domain.product.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 같은 디자인의 색상/사이즈 형제 제품들 (제품 상세의 colors/sizes 구성용)
    List<Product> findByProductGroupIdOrderByIdAsc(Long productGroupId);

    // 시즌 제품 목록 (season 컬럼 값은 스토리 도메인과 동일한 "SS/AW{연도}" 형식, 예: AW2026)
    List<Product> findBySeasonOrderByIdAsc(String season);

    // 베스트셀러(노출 순서 상위 10건) - COUNT 쿼리 없이 DB에서 바로 상한 적용
    List<Product> findTop10ByOrderByIdAsc();
}
