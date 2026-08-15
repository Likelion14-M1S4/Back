package com.meisterbear.domain.user.repository;

import com.meisterbear.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKakaoId(Long kakaoId);

    Optional<User> findByEmail(String email);
}
