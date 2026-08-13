package com.meisterbear.domain.wishlist.service;

import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.exception.CharmErrorCode;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.exception.ProductErrorCode;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.wishlist.dto.request.ToggleWishlistRequest;
import com.meisterbear.domain.wishlist.dto.response.ToggleWishlistResultResponse;
import com.meisterbear.domain.wishlist.dto.response.WishlistItemResponse;
import com.meisterbear.domain.wishlist.dto.response.WishlistListResponse;
import com.meisterbear.domain.wishlist.entity.Wishlist;
import com.meisterbear.domain.wishlist.entity.WishlistItemType;
import com.meisterbear.domain.wishlist.exception.WishlistErrorCode;
import com.meisterbear.domain.wishlist.repository.WishlistRepository;
import com.meisterbear.global.exception.CustomException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final CharmRepository charmRepository;

    public WishlistListResponse findWishlist(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderBySavedAtDesc(userId);
        if (wishlists.isEmpty()) {
            log.info("[WishlistService] 위시리스트 조회 완료(찜한 항목 없음) - userId={}", userId);
            return WishlistListResponse.empty();
        }

        Map<Long, Product> productsById = productRepository.findAllById(wishlists.stream()
                        .map(Wishlist::getProductId)
                        .filter(Objects::nonNull)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Charm> charmsById = charmRepository.findAllById(wishlists.stream()
                        .map(Wishlist::getCharmId)
                        .filter(Objects::nonNull)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Charm::getId, Function.identity()));

        // 찜한 제품/참이 그사이 삭제됐을 수 있음 - 그런 항목은 목록에서 조용히 제외 (에러로 취급하지 않음)
        List<WishlistItemResponse> items = wishlists.stream()
                .map(wishlist -> toWishlistItemResponse(wishlist, productsById, charmsById))
                .filter(Objects::nonNull)
                .toList();

        log.info("[WishlistService] 위시리스트 조회 완료 - userId={}, itemCount={}", userId, items.size());
        return WishlistListResponse.builder()
                .items(items)
                .build();
    }

    // productId, charmId 중 정확히 하나만 받아 이미 찜한 상태면 해제(삭제), 아니면 찜(생성)한다.
    // 제품/참 상세의 하트 아이콘, 위시리스트 목록의 X 버튼이 이 메서드 하나를 공유한다.
    @Transactional
    public ToggleWishlistResultResponse toggleWishlist(Long userId, ToggleWishlistRequest request) {
        Long productId = request.getProductId();
        Long charmId = request.getCharmId();
        if ((productId == null) == (charmId == null)) {
            throw new CustomException(WishlistErrorCode.INVALID_TARGET);
        }

        if (productId != null) {
            return toggleProduct(userId, productId);
        }
        return toggleCharm(userId, charmId);
    }

    private ToggleWishlistResultResponse toggleProduct(Long userId, Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new CustomException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
        boolean wished = applyToggle(existing, () -> Wishlist.builder().userId(userId).productId(productId).build());

        log.info("[WishlistService] 제품 찜 토글 완료 - userId={}, productId={}, isWished={}", userId, productId, wished);
        return ToggleWishlistResultResponse.builder()
                .type(WishlistItemType.PRODUCT)
                .targetId(productId)
                .wished(wished)
                .build();
    }

    private ToggleWishlistResultResponse toggleCharm(Long userId, Long charmId) {
        if (!charmRepository.existsById(charmId)) {
            throw new CustomException(CharmErrorCode.CHARM_NOT_FOUND);
        }

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndCharmId(userId, charmId);
        boolean wished = applyToggle(existing, () -> Wishlist.builder().userId(userId).charmId(charmId).build());

        log.info("[WishlistService] 참 찜 토글 완료 - userId={}, charmId={}, isWished={}", userId, charmId, wished);
        return ToggleWishlistResultResponse.builder()
                .type(WishlistItemType.CHARM)
                .targetId(charmId)
                .wished(wished)
                .build();
    }

    // 기존 찜이 있으면 삭제(해제)하고 false, 없으면 새로 만들고(찜) true를 반환
    private boolean applyToggle(Optional<Wishlist> existing, Supplier<Wishlist> newWishlist) {
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        }
        wishlistRepository.save(newWishlist.get());
        return true;
    }

    private WishlistItemResponse toWishlistItemResponse(Wishlist wishlist, Map<Long, Product> productsById,
                                                          Map<Long, Charm> charmsById) {
        if (wishlist.getProductId() != null) {
            Product product = productsById.get(wishlist.getProductId());
            if (product == null) {
                log.warn("[WishlistService] 찜한 제품이 존재하지 않아 제외 - wishlistId={}, productId={}",
                        wishlist.getId(), wishlist.getProductId());
                return null;
            }
            return WishlistItemResponse.builder()
                    .id(wishlist.getId())
                    .type(WishlistItemType.PRODUCT)
                    .targetId(product.getId())
                    .name(product.getName())
                    .imgUrl(product.getImgUrl())
                    .price(product.getPrice())
                    .build();
        }

        Charm charm = charmsById.get(wishlist.getCharmId());
        if (charm == null) {
            log.warn("[WishlistService] 찜한 참이 존재하지 않아 제외 - wishlistId={}, charmId={}",
                    wishlist.getId(), wishlist.getCharmId());
            return null;
        }
        return WishlistItemResponse.builder()
                .id(wishlist.getId())
                .type(WishlistItemType.CHARM)
                .targetId(charm.getId())
                .name(charm.getName())
                .imgUrl(charm.getImgUrl())
                .price(charm.getPrice())
                .build();
    }
}
