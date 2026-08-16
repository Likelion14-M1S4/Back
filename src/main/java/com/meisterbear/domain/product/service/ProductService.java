package com.meisterbear.domain.product.service;

import com.meisterbear.domain.order.repository.OrderItemRepository;
import com.meisterbear.domain.product.dto.response.BestsellerSectionResponse;
import com.meisterbear.domain.product.dto.response.CurationSectionResponse;
import com.meisterbear.domain.product.dto.response.JourneySectionResponse;
import com.meisterbear.domain.product.dto.response.ProductColorResponse;
import com.meisterbear.domain.product.dto.response.ProductDetailResponse;
import com.meisterbear.domain.product.dto.response.ProductDetailSectionResponse;
import com.meisterbear.domain.product.dto.response.ProductSummaryResponse;
import com.meisterbear.domain.product.dto.response.RecommendPageResponse;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.exception.ProductErrorCode;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.wishlist.repository.WishlistRepository;
import com.meisterbear.global.exception.CustomException;
import java.util.ArrayList;
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

    // 제품 상세 매장 확인 버튼 구성값 (프론트 mock과 동일)
    private static final String STORE_CHECK_LABEL = "구매 가능 매장 확인하기";
    private static final String STORE_CHECK_URL = "/story/stores";

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderItemRepository orderItemRepository;

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

    // 제품 상세. 색상/사이즈 옵션은 같은 productGroupId 형제 제품에서 구성하고,
    // 찜/구매 여부는 로그인 유저 기준으로 판단한다. 시즌 제품 상세 화면도 이 API를 함께 쓴다
    public ProductDetailResponse getProductDetail(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<Product> siblings = findGroupSiblings(product);
        boolean isLiked = wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent();
        boolean isPurchased = orderItemRepository.existsByUserIdAndProductId(userId, productId);

        log.info("[ProductService] 제품 상세 조회 완료 - productId={}, siblings={}", productId, siblings.size());
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImgUrl())
                .isLiked(isLiked)
                .colorLabel(product.getColor())
                .colors(toColorOptions(siblings))
                .sizes(toSizeOptions(siblings))
                .selectedSize(product.getSize())
                .storeCheckLabel(STORE_CHECK_LABEL)
                .storeUrl(STORE_CHECK_URL)
                .detail(toDetailSection(product))
                .isPurchased(isPurchased)
                // 시즌 값이 있는 제품 = 시즌 한정 → 스토리 완주 후 구매 가능 정책
                .requiresStory(product.getSeason() != null)
                .build();
    }

    // 그룹이 없는 단독 제품은 자기 자신만 옵션으로 노출한다
    private List<Product> findGroupSiblings(Product product) {
        if (product.getProductGroupId() == null) {
            return List.of(product);
        }
        return productRepository.findByProductGroupIdOrderByIdAsc(product.getProductGroupId());
    }

    // 색상 옵션: 형제 중 색상이 처음 등장하는 제품만 노출 (같은 색의 사이즈 형제는 중복 제거)
    private List<ProductColorResponse> toColorOptions(List<Product> siblings) {
        List<ProductColorResponse> colors = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Product sibling : siblings) {
            if (seen.contains(sibling.getColor())) {
                continue;
            }
            seen.add(sibling.getColor());
            colors.add(ProductColorResponse.builder()
                    .id(sibling.getId())
                    .name(sibling.getColor())
                    .imageUrl(sibling.getImgUrl())
                    .build());
        }
        return colors;
    }

    private List<String> toSizeOptions(List<Product> siblings) {
        return siblings.stream()
                .map(Product::getSize)
                .distinct()
                .toList();
    }

    private ProductDetailSectionResponse toDetailSection(Product product) {
        List<String> specs = new ArrayList<>();
        addSpec(specs, "소재", product.getMaterial());
        addSpec(specs, "아틀리에", product.getAtelier());
        addSpec(specs, "원산지", product.getRegion());
        addSpec(specs, "카테고리", product.getCategory());
        return ProductDetailSectionResponse.builder()
                .headline(product.getName())
                .description(product.getDetail())
                .specs(specs)
                .build();
    }

    private void addSpec(List<String> specs, String label, String value) {
        if (value != null && !value.isBlank()) {
            specs.add(label + ": " + value);
        }
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
