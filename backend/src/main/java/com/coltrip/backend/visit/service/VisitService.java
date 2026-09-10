package com.coltrip.backend.visit.service;

import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.common.util.GeoUtils;
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
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import com.coltrip.backend.visit.exception.AlreadyOngoingVisitException;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitConditionNotMetException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
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
