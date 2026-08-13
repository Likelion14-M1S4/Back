package com.meisterbear.domain.wishlist.service;

import com.meisterbear.domain.charm.entity.Charm;
import com.meisterbear.domain.charm.repository.CharmRepository;
import com.meisterbear.domain.product.entity.Product;
import com.meisterbear.domain.product.repository.ProductRepository;
import com.meisterbear.domain.wishlist.dto.response.WishlistItemResponse;
import com.meisterbear.domain.wishlist.dto.response.WishlistListResponse;
import com.meisterbear.domain.wishlist.entity.Wishlist;
import com.meisterbear.domain.wishlist.entity.WishlistItemType;
import com.meisterbear.domain.wishlist.repository.WishlistRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
