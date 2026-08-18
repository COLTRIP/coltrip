package com.coltrip.backend.user.service;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final VisitRepository visitRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse updateNickname(Long userId, String nickname) {
        User user = findUser(userId);
        user.updateNickname(nickname);
        return UserResponse.from(user);
    }

    // 하드 삭제: User row와 연관된 Visit 이력을 모두 제거 (2026-08-19 확정)
    public void deleteMe(Long userId) {
        User user = findUser(userId);
        visitRepository.deleteByUser_Id(userId);
        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UnauthorizedException::new);
    }
}
