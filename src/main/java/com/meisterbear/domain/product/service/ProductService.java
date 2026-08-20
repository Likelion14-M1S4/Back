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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final String IMAGE_BASE = "https://meisterbear-images.s3.ap-northeast-2.amazonaws.com";
    private static final String RECOMMEND_HERO_IMAGE = IMAGE_BASE + "/recommend/hero.png";
    private static final String RECOMMEND_HERO_LINK = "/recommend/charms";
    private static final String JOURNEY_TITLE = "마이스터베어와 함께하는 여정";
    private static final String JOURNEY_SUBTITLE = "나만의 참과 캐릭터를 찾아보세요";
    private static final String CURATION_TITLE = "이달의 큐레이션";
    private static final String CURATION_IMAGE = IMAGE_BASE + "/recommend/curation.png";
    private static final String BESTSELLER_TITLE = "베스트셀러";

    private static final String STORE_CHECK_LABEL = "구매 가능 매장 확인하기";
    private static final String STORE_CHECK_URL = "/story/stores";

    private static final String SEASON_HERO_IMAGE_FORMAT = IMAGE_BASE + "/season/%s/hero.png";
    private static final String SEASON_DESCRIPTION = "마이스터베어의 새로운 시즌을 만나보세요.";

    private final ProductRepository productRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;

    public RecommendPageResponse getRecommendPage() {
        List<ProductSummaryResponse> bestsellers = productRepository.findTop10ByOrderByIdAsc().stream()
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
                .requiresStory(product.getSeason() != null)
                .build();
    }

    public List<MyProductResponse> getMyProducts(Long userId) {
        List<OrderItem> orders = orderItemRepository.findByUserId(userId);
        Map<Long, Product> products = findProductsById(orders.stream().map(OrderItem::getProductId).toList());

        List<MyProductResponse> responses = orders.stream()
                .sorted(java.util.Comparator.comparing(this::resolveRegisteredAt)
                        .thenComparing(OrderItem::getId)
                        .reversed())
                .map(order -> {
                    Product product = products.get(order.getProductId());
                    return MyProductResponse.builder()
                            .id(order.getId())
                            .name(product != null ? product.getName() : null)
                            .imageUrl(product != null ? product.getImgUrl() : null)
                            .registeredAt(formatDate(resolveRegisteredAt(order)))
                            .build();
                })
                .toList();
        log.info("[ProductService] 등록 제품 목록 조회 완료 - userId={}, count={}", userId, responses.size());
        return responses;
    }

    public MyProductDetailResponse getMyProductDetail(Long userId, Long orderItemId) {
        OrderItem order = orderItemRepository.findByIdAndUserId(orderItemId, userId)
                .orElseThrow(() -> new CustomException(ProductErrorCode.ORDER_NOT_FOUND));
        Product product = productRepository.findById(order.getProductId()).orElse(null);
        String storeName = storeRepository.findById(order.getStoreId())
                .map(Store::getName)
                .orElse(null);

        LocalDateTime registeredAt = resolveRegisteredAt(order);
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

    private LocalDateTime resolveRegisteredAt(OrderItem order) {
        return order.getReceivedAt() != null ? order.getReceivedAt() : order.getOrderedAt();
    }

    private Map<Long, Product> findProductsById(List<Long> productIds) {
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMAT) : null;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        int hour = dateTime.getHour();
        String meridiem = hour < 12 ? "am" : "pm";
        int hour12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format("%s %s.%02d:%02d", dateTime.format(DATE_FORMAT), meridiem, hour12, dateTime.getMinute());
    }

    // 시즌 값이 잘못 와도 에러 대신 빈 목록으로 내려준다
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

    private List<Product> findGroupSiblings(Product product) {
        if (product.getProductGroupId() == null) {
            return List.of(product);
        }
        return productRepository.findByProductGroupIdOrderByIdAsc(product.getProductGroupId());
    }

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
