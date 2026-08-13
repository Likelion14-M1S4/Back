package com.meisterbear.domain.wishlist.repository;

import com.meisterbear.domain.wishlist.entity.Wishlist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserIdOrderBySavedAtDesc(Long userId);
}
