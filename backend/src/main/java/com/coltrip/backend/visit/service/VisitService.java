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

    // 잠정값, 팀 확정 필요 - docs/schema.md, task.md 참고
    private static final long REQUIRED_STAY_DURATION_SECONDS = 600;

    private final VisitRepository visitRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public VisitStartResponse start(Long userId, VisitStartRequest request) {
        if (visitRepository.existsByUser_IdAndStatus(userId, VisitStatus.STARTED)) {
            throw new AlreadyOngoingVisitException();
        }

        User user = userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
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
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(VisitNotFoundException::new);

        if (!visit.getUser().getId().equals(userId)) {
            throw new VisitNotFoundException();
        }
        if (visit.getStatus() != VisitStatus.STARTED) {
            throw new InvalidVisitStateException();
        }

        validateCondition(visit, request);

        visit.markArrived();
        visit.complete();

        return VisitCompleteResponse.from(visit);
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

        boolean withinRadius = distance <= spot.getCategory().getVisitRadiusMeters();
        boolean stayedLongEnough = request.stayDurationSeconds() >= REQUIRED_STAY_DURATION_SECONDS;

        if (!withinRadius || !stayedLongEnough) {
            throw new VisitConditionNotMetException();
        }
    }
}
