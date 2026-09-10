package com.coltrip.backend.user.service;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStatsReader {

    private final VisitRepository visitRepository;
    private final SpotLikeRepository spotLikeRepository;

    public UserResponse toResponse(User user) {
        long visitCount = visitRepository.countByUser_IdAndStatus(
                user.getId(), VisitStatus.COMPLETED);
        long likeCount = spotLikeRepository.countByUser_Id(user.getId());

        Long currentVisitId = visitRepository
                .findByUserIdAndStatusWithSpot(user.getId(), VisitStatus.STARTED)
                .stream()
                .findFirst()
                .map(visit -> visit.getId())
                .orElse(null);

        return UserResponse.of(user, visitCount, likeCount, currentVisitId);
    }
}
