package com.meisterbear.domain.user.service;

import com.meisterbear.domain.user.dto.request.UpdateUserRequest;
import com.meisterbear.domain.user.dto.response.UserResponse;
import com.meisterbear.domain.user.entity.User;
import com.meisterbear.domain.user.exception.UserErrorCode;
import com.meisterbear.domain.user.repository.UserRepository;
import com.meisterbear.global.exception.CustomException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // YYYY-MM-DD 형식만 확인, 실제 날짜 유효성은 검사하지 않음
    private static final Pattern BIRTH_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private final UserRepository userRepository;

    public UserResponse findMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        log.info("[UserService] 내 정보 조회 완료 - userId={}", userId);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String nickname = request.getNickname() != null ? request.getNickname() : user.getNickname();
        String phone = request.getPhone() != null ? normalizePhone(request.getPhone()) : user.getPhone();
        String birthDate = request.getBirthDate() != null
                ? validateBirthDate(request.getBirthDate())
                : user.getBirthDate();
        user.updateProfile(nickname, phone, birthDate);

        log.info("[UserService] 내 정보 수정 완료 - userId={}", userId);
        return toUserResponse(user);
    }

    // 10자리면 3-3-4, 11자리면 3-4-4로 공백 구분 (예: 010 9973 7761)
    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + " " + digits.substring(3, 7) + " " + digits.substring(7);
        }
        throw new CustomException(UserErrorCode.INVALID_PHONE_FORMAT);
    }

    private String validateBirthDate(String birthDate) {
        if (!BIRTH_DATE_PATTERN.matcher(birthDate).matches()) {
            throw new CustomException(UserErrorCode.INVALID_BIRTH_DATE_FORMAT);
        }
        return birthDate;
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .build();
    }
}
