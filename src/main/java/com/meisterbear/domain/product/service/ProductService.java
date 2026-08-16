package com.meisterbear.domain.product.service;

import com.meisterbear.domain.order.entity.OrderItem;
import com.meisterbear.domain.order.repository.OrderItemRepository;
import com.meisterbear.domain.product.dto.response.BestsellerSectionResponse;
import com.meisterbear.domain.product.dto.response.CurationSectionResponse;
import com.meisterbear.domain.product.dto.response.JourneySectionResponse;
import com.meisterbear.domain.product.dto.response.MyProductDetailResponse;
import com.meisterbear.domain.product.dto.response.MyProductResponse;
import com.meisterbear.domain.product.dto.response.ProductColorResponse;
import com.meisterbear.domain.product.dto.response.ProductDetailResponse;
import com.meisterbear.domain.product.dto.response.ProductDetailSectionResponse;
import com.meisterbear.domain.product.dto.response.ProductSummaryResponse;
import com.meisterbear.domain.product.dto.response.RecommendPageResponse;
import com.meisterbear.domain.product.dto.response.SeasonProductListResponse;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.exception.ProductErrorCode;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.store.entity.Store;
import com.meisterbear.domain.store.repository.StoreRepository;
import com.meisterbear.domain.wishlist.repository.WishlistRepository;
import com.meisterbear.global.exception.CustomException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    // 시즌 페이지 구성값. 시즌 문자열은 시드의 product.season 값과 일치해야 한다 (현재 시즌: 2026-FALL)
    private static final String SEASON_HERO_IMAGE_FORMAT = IMAGE_BASE + "/season/%s/hero.png";
    // 어떤 시즌을 조회해도 어긋나지 않도록 시즌 중립 문구를 쓴다 (시즌별 카피가 확정되면 시즌→문구 맵으로 확장)
    private static final String SEASON_DESCRIPTION = "마이스터베어의 새로운 시즌을 만나보세요.";

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;

    // 추천 페이지 한 방 조회. 히어로/여정/큐레이션은 고정 구성, 베스트셀러만 DB에서 채운다.
    // 베스트셀러 기준 = 노출 순서(시드 id 순) - 시연 데이터에는 판매량 집계가 무의미해 채택한 기준.
    // 정렬과 상한을 DB 쿼리에 적용해 결정론과 메모리 사용을 보장한다
    public RecommendPageResponse getRecommendPage() {
        List<ProductSummaryResponse> bestsellers = productRepository
                .findAll(PageRequest.of(0, BESTSELLER_LIMIT, Sort.by("id")))
                .getContent().stream()
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

    // 등록(구매) 제품 목록 - 최근 구매 순. 날짜는 화면 표기 포맷 문자열로 완성해서 내려준다 (프론트 가공 불필요)
    public List<MyProductResponse> getMyProducts(Long userId) {
        List<OrderItem> orders = orderItemRepository.findByUserIdOrderByOrderedAtDesc(userId);
        Map<Long, Product> products = findProductsById(orders.stream().map(OrderItem::getProductId).toList());

        List<MyProductResponse> responses = orders.stream()
                .map(order -> {
                    Product product = products.get(order.getProductId());
                    return MyProductResponse.builder()
                            .id(order.getId())
                            .name(product != null ? product.getName() : null)
                            .imageUrl(product != null ? product.getImgUrl() : null)
                            .registeredAt(formatDate(order.getOrderedAt()))
                            .build();
                })
                .toList();
        log.info("[ProductService] 등록 제품 목록 조회 완료 - userId={}, count={}", userId, responses.size());
        return responses;
    }

    // 등록 제품 상세. 본인 구매 기록만 조회 가능(userId 대조). registeredAt은 수령 시점, 미수령이면 구매 시점으로 대체
    public MyProductDetailResponse getMyProductDetail(Long userId, Long orderItemId) {
        OrderItem order = orderItemRepository.findByIdAndUserId(orderItemId, userId)
                .orElseThrow(() -> new CustomException(ProductErrorCode.ORDER_NOT_FOUND));
        Product product = productRepository.findById(order.getProductId()).orElse(null);
        String storeName = storeRepository.findById(order.getStoreId())
                .map(Store::getName)
                .orElse(null);

        LocalDateTime registeredAt = order.getReceivedAt() != null ? order.getReceivedAt() : order.getOrderedAt();
        log.info("[ProductService] 등록 제품 상세 조회 완료 - userId={}, orderItemId={}", userId, orderItemId);
        return MyProductDetailResponse.builder()
                .id(order.getId())
                .name(product != null ? product.getName() : null)
                .colorLabel(product != null ? product.getColor() : null)
                .sizeLabel(product != null ? product.getSize() : null)
                .imageUrl(product != null ? product.getImgUrl() : null)
                .purchasedAt(formatDateTime(order.getOrderedAt()))
                .registeredAt(formatDateTime(registeredAt))
                .storeName(storeName)
                .build();
    }

    // N+1 방지를 위한 일괄 조회 (주문 목록 → 제품 맵)
    private Map<Long, Product> findProductsById(List<Long> productIds) {
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    // "2026.08.16" - 목록/이력의 날짜 표기 포맷
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMAT) : null;
    }

    // "2026.08.16 pm.03:00" - 프론트 화면이 쓰는 날짜+시간 표기 포맷 (12시간제, am/pm 소문자)
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        int hour = dateTime.getHour();
        String meridiem = hour < 12 ? "am" : "pm";
        int hour12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%s %s.%02d:%02d", dateTime.format(DATE_FORMAT), meridiem, hour12, dateTime.getMinute());
    }

    // 시즌 제품 목록. 시즌 값이 잘못 와도(오타 등) 에러 대신 빈 목록으로 내려 화면이 깨지지 않게 한다 (시연 우선)
    public SeasonProductListResponse getSeasonProducts(String season) {
        List<ProductSummaryResponse> products = productRepository.findBySeasonOrderByIdAsc(season).stream()
                .map(this::toSummary)
                .toList();

        log.info("[ProductService] 시즌 제품 목록 조회 완료 - season={}, count={}", season, products.size());
        return SeasonProductListResponse.builder()
                .heroImageUrl(String.format(SEASON_HERO_IMAGE_FORMAT, season))
                .description(SEASON_DESCRIPTION)
                .products(products)
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
