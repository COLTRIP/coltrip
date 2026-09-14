package com.coltrip.backend.user.service;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.review.ReviewRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.user.dto.NotificationSettingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserStatsReader userStatsReader;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return userStatsReader.toResponse(findUser(userId));
    }

    public UserResponse updateNickname(Long userId, String nickname) {
        User user = findUserForUpdate(userId);
        user.updateNickname(nickname);
        return userStatsReader.toResponse(user);
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings(Long userId) {
        return NotificationSettingsResponse.from(findUser(userId));
    }

    public NotificationSettingsResponse updateNotificationSettings(Long userId, boolean enabled) {
        User user = findUserForUpdate(userId);
        user.updateAlternativeNotificationEnabled(enabled);
        return NotificationSettingsResponse.from(user);
    }

    // 하드 삭제: User row와 연관 데이터를 모두 제거 (2026-08-19 확정)
    // Review가 Visit을 참조하므로 Review -> Like -> Visit -> User 순서로 지워야 FK 제약에 걸리지 않는다
    public void deleteMe(Long userId) {
        User user = findUserForUpdate(userId);
        reviewRepository.deleteByUser_Id(userId);
        spotLikeRepository.deleteByUser_Id(userId);
        visitRepository.deleteByUser_Id(userId);
        userRepository.delete(user);
    }

    // 인증/방문/좋아요와 같은 잠금 순서를 사용해 오래된 사용자 상태의 덮어쓰기를 방지한다.
    private User findUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(UnauthorizedException::new);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UnauthorizedException::new);
    }
}
