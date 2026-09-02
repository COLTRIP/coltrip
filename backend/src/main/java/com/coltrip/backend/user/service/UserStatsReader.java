package com.coltrip.backend.user.service;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 로그인 응답과 내 정보 조회가 같은 기준으로 개수를 세도록 한 곳에 모아둔다.
// visitCount는 실제로 다녀온 곳만 세므로 COMPLETED만 집계한다(STARTED 제외).
@Component
@RequiredArgsConstructor
public class UserStatsReader {

    private final VisitRepository visitRepository;
    private final SpotLikeRepository spotLikeRepository;

    public UserResponse toResponse(User user) {
        long visitCount = visitRepository.countByUser_IdAndStatus(user.getId(), VisitStatus.COMPLETED);
        long likeCount = spotLikeRepository.countByUser_Id(user.getId());
        return UserResponse.of(user, visitCount, likeCount);
    }
}
