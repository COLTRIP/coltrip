package com.coltrip.backend.user.service;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse updateNickname(Long userId, String nickname) {
        User user = findUser(userId);
        user.updateNickname(nickname);
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UnauthorizedException::new);
    }
}
