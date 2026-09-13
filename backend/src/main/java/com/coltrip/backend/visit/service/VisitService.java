package com.coltrip.backend.visit.service;

import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.common.util.GeoUtils;
import com.coltrip.backend.domain.review.Review;
import com.coltrip.backend.domain.review.ReviewRepository;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import com.coltrip.backend.visit.dto.VisitCancelResponse;
import com.coltrip.backend.visit.dto.VisitCompleteRequest;
import com.coltrip.backend.visit.dto.VisitCompleteResponse;
import com.coltrip.backend.visit.dto.VisitHistoryResponse;
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import com.coltrip.backend.visit.exception.AlreadyOngoingVisitException;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitConditionNotMetException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.coltrip.backend.visit.dto.CurrentVisitResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitService {

    private final VisitRepository visitRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public VisitStartResponse start(Long userId, VisitStartRequest request) {
        // 사용자 행에 쓰기 잠금을 먼저 걸어 같은 사용자의 동시 요청을 직렬화한다.
        // 이 잠금이 없으면 두 요청이 모두 아래 존재 여부 조회를 통과해 STARTED 방문이 중복 생성될 수 있다.
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(UnauthorizedException::new);

        if (visitRepository.existsByUser_IdAndStatus(userId, VisitStatus.STARTED)) {
            throw new AlreadyOngoingVisitException();
        }

        TouristSpot spot = touristSpotRepository.findById(request.spotId())
                .orElseThrow(SpotNotFoundException::new);

        Visit visit = visitRepository.save(Visit.builder()
                .user(user)
                .spot(spot)
                .startLatitude(request.startLatitude())
                .startLongitude(request.startLongitude())
                .build());

        return VisitStartResponse.from(visit);
    }

    public VisitCompleteResponse complete(Long userId, Long visitId, VisitCompleteRequest request) {
        Visit visit = findOwnedStartedVisit(userId, visitId);

        validateCondition(visit, request);

        visit.markArrived();
        visit.complete();

        return VisitCompleteResponse.from(visit);
    }

    // 대체지 선택 등으로 목적지를 바꿀 때, 진행 중이던 방문을 중단하고 새 방문을 시작할 수 있게 한다.
    public VisitCancelResponse cancel(Long userId, Long visitId) {
        Visit visit = findOwnedStartedVisit(userId, visitId);

        visit.cancel();

        return VisitCancelResponse.from(visit);
    }

    private Visit findOwnedStartedVisit(Long userId, Long visitId) {
        // 시작/완료/취소/대체지 선택은 동일한 사용자 행 잠금 순서를 사용한다.
        userRepository.findByIdForUpdate(userId).orElseThrow(UnauthorizedException::new);
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(VisitNotFoundException::new);

        if (!visit.getUser().getId().equals(userId)) {
            throw new VisitNotFoundException();
        }
        if (visit.getStatus() != VisitStatus.STARTED) {
            throw new InvalidVisitStateException();
        }
        return visit;
    }

    @Transactional(readOnly = true)
    public CurrentVisitResponse getCurrent(Long userId) {
        return visitRepository.findByUserIdAndStatusWithSpot(userId, VisitStatus.STARTED).stream()
                .findFirst()
                .map(CurrentVisitResponse::from)
                .orElseGet(CurrentVisitResponse::empty);
    }

    // 마이페이지 "다녀온 곳" 목록. 완료순(최신 완료 먼저)으로 정렬, 각 방문의 리뷰 작성 여부(reviewId)를 함께 내려준다.
    @Transactional(readOnly = true)
    public VisitHistoryResponse getHistory(Long userId) {
        List<Visit> visits = visitRepository.findByUserIdAndStatusOrderByCompletedAtDesc(userId, VisitStatus.COMPLETED);
        if (visits.isEmpty()) {
            return VisitHistoryResponse.empty();
        }

        List<Long> visitIds = visits.stream().map(Visit::getId).toList();
        var reviewIdByVisitId = reviewRepository.findByVisit_IdIn(visitIds).stream()
                .collect(Collectors.toMap(review -> review.getVisit().getId(), Review::getId));

        return VisitHistoryResponse.from(visits, reviewIdByVisitId);
    }

    private void validateCondition(Visit visit, VisitCompleteRequest request) {
        TouristSpot spot = visit.getSpot();
        double distance = GeoUtils.distanceMeters(
                spot.getLatitude(), spot.getLongitude(),
                request.arrivedLatitude(), request.arrivedLongitude());

        if (distance > spot.getCategory().getVisitRadiusMeters()) {
            throw new VisitConditionNotMetException();
        }
    }
}
