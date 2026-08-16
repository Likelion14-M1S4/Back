package com.meisterbear.domain.product.service;

import com.meisterbear.domain.product.dto.response.BestsellerSectionResponse;
import com.meisterbear.domain.product.dto.response.CurationSectionResponse;
import com.meisterbear.domain.product.dto.response.JourneySectionResponse;
import com.meisterbear.domain.product.dto.response.ProductSummaryResponse;
import com.meisterbear.domain.product.dto.response.RecommendPageResponse;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    // 추천 페이지 구성 문구/이미지. 기획 확정 카피가 오면 여기만 바꾸면 된다 (별도 테이블을 둘 만큼 유동적이지 않음)
    private static final String IMAGE_BASE = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com";
    private static final String RECOMMEND_HERO_IMAGE = IMAGE_BASE + "/recommend/hero.png";
    private static final String RECOMMEND_HERO_LINK = "/recommend/charms";
    private static final String JOURNEY_TITLE = "마이스터베어와 함께하는 여정";
    private static final String JOURNEY_SUBTITLE = "나만의 참과 캐릭터를 찾아보세요";
    private static final String CURATION_TITLE = "이달의 큐레이션";
    private static final String CURATION_IMAGE = IMAGE_BASE + "/recommend/curation.png";
    private static final String BESTSELLER_TITLE = "베스트셀러";
    // 시연 데이터 규모(수십 건)에서 노출 개수만 제한하는 값
    private static final int BESTSELLER_LIMIT = 10;

    private final ProductRepository productRepository;

    // 추천 페이지 한 방 조회. 히어로/여정/큐레이션은 고정 구성, 베스트셀러만 DB에서 채운다
    public RecommendPageResponse getRecommendPage() {
        List<ProductSummaryResponse> bestsellers = productRepository.findAll().stream()
                .limit(BESTSELLER_LIMIT)
                .map(this::toSummary)
                .toList();

        log.info("[ProductService] 추천 페이지 조회 완료 - bestsellers={}", bestsellers.size());
        return RecommendPageResponse.builder()
                .heroImageUrl(RECOMMEND_HERO_IMAGE)
                .heroLinkTo(RECOMMEND_HERO_LINK)
                .journey(JourneySectionResponse.builder()
                        .title(JOURNEY_TITLE)
                        .subtitle(JOURNEY_SUBTITLE)
                        .build())
                .curation(CurationSectionResponse.builder()
                        .title(CURATION_TITLE)
                        .imageUrl(CURATION_IMAGE)
                        .build())
                .bestsellers(BestsellerSectionResponse.builder()
                        .title(BESTSELLER_TITLE)
                        .products(bestsellers)
                        .build())
                .build();
    }

    private ProductSummaryResponse toSummary(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImgUrl())
                .build();
    }
}
