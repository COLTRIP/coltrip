package com.coltrip.backend.like.service;

import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.like.SpotLike;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.like.dto.LikeResponse;
import com.coltrip.backend.spot.dto.SpotListResponse;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SpotLikeService {

    private final SpotLikeRepository spotLikeRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final UserRepository userRepository;

    // 이미 눌러둔 상태에서 또 호출해도 성공 응답(멱등). 따닥 눌러도 에러가 안 나게.
    public LikeResponse like(Long userId, Long spotId) {
        // 중복 확인 전에 잠가 동시 등록/취소를 같은 사용자 단위로 직렬화한다.
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(UnauthorizedException::new);
        if (spotLikeRepository.existsByUser_IdAndSpot_Id(userId, spotId)) {
            return new LikeResponse(spotId, true);
        }

        TouristSpot spot = touristSpotRepository.findById(spotId).orElseThrow(SpotNotFoundException::new);

        spotLikeRepository.save(SpotLike.builder().user(user).spot(spot).build());
        return new LikeResponse(spotId, true);
    }

    public LikeResponse unlike(Long userId, Long spotId) {
        userRepository.findByIdForUpdate(userId).orElseThrow(UnauthorizedException::new);
        spotLikeRepository.findByUser_IdAndSpot_Id(userId, spotId)
                .ifPresent(spotLikeRepository::delete);
        return new LikeResponse(spotId, false);
    }

    @Transactional(readOnly = true)
    public SpotListResponse findMyLikedSpots(Long userId) {
        return SpotListResponse.allLiked(spotLikeRepository.findLikedSpotsByUserId(userId));
    }
}
