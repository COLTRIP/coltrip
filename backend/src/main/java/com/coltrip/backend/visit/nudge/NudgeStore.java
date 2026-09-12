package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.alternative.dto.AlternativeListResponse;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import com.coltrip.backend.visit.nudge.NudgeResponse.Proposal;
import com.coltrip.backend.visit.service.VisitService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class NudgeStore {
    private final VisitRepository visits;
    private final UserRepository users;
    private final VisitService visitService;
    private final NudgePolicy policy;
    private final ObjectMapper mapper;
    private final Clock clock;

    public NudgeStore(VisitRepository visits, UserRepository users, VisitService visitService,
                      NudgePolicy policy, ObjectMapper mapper,
                      @Qualifier("alternativeClock") Clock clock) {
        this.visits = visits;
        this.users = users;
        this.visitService = visitService;
        this.policy = policy;
        this.mapper = mapper;
        this.clock = clock;
    }

    public Plan prepare(Long userId, Long visitId) {
        Visit visit = ownedLocked(userId, visitId);
        NudgeResponse response = existingOrBlocked(visit, now());
        return new Plan(visit.getSpot().getId(), visit.getSpot().getCurrentQuietScore(),
                visit.getSpot().getQuietScoreUpdatedAt(), response);
    }

    // AI 응답을 기다린 뒤 새 트랜잭션에서 방문 상태와 점수를 다시 확인한다.
    public NudgeResponse publish(Long userId, Long visitId, Plan plan, AlternativeListResponse result) {
        Visit visit = ownedLocked(userId, visitId);
        LocalDateTime now = now();
        NudgeResponse existing = existingOrBlocked(visit, now);
        if (existing != null) {
            return existing;
        }
        if (!Objects.equals(plan.observedAt(), visit.getSpot().getQuietScoreUpdatedAt())
                || !Objects.equals(plan.currentScore(), visit.getSpot().getCurrentQuietScore())) {
            return NudgeResponse.state("SCORE_CHANGED");
        }
        if (!result.triggered()) {
            return NudgeResponse.state("AI_NOT_TRIGGERED");
        }
        if (result.alternatives().isEmpty()) {
            return NudgeResponse.state("NO_ALTERNATIVES");
        }
        Proposal proposal = new Proposal(visitId, plan.spotId(), visit.getStartQuietScore(),
                plan.currentScore(), plan.observedAt(), now, policy.expiresAt(now), result.alternatives());
        visit.saveAlternativeSuggestion(mapper.writeValueAsString(proposal));
        return new NudgeResponse("OFFERED", proposal);
    }

    public NudgeResponse dismiss(Long userId, Long visitId) {
        Visit visit = ownedLocked(userId, visitId);
        if (visit.getStatus() != VisitStatus.STARTED) {
            throw new InvalidVisitStateException();
        }
        visit.dismissAlternativeSuggestion(now());
        return NudgeResponse.state("DISMISSED");
    }

    // 기존 방문 취소와 새 방문 시작을 같은 트랜잭션에서 수행한다.
    public VisitStartResponse select(Long userId, Long visitId, NudgeSelectRequest request) {
        Visit visit = ownedLocked(userId, visitId);
        if (visit.getAlternativeSelectedVisitId() != null) {
            Visit selected = visits.findById(visit.getAlternativeSelectedVisitId())
                    .orElseThrow(VisitNotFoundException::new);
            if (!selected.getUser().getId().equals(userId)
                    || !selected.getSpot().getId().equals(request.spotId())) {
                throw new InvalidVisitStateException();
            }
            return VisitStartResponse.from(selected);
        }
        NudgeResponse response = existingOrBlocked(visit, now());
        if (response == null || !"OFFERED".equals(response.state())
                || response.proposal().alternatives().stream()
                        .noneMatch(item -> item.spot().id().equals(request.spotId()))) {
            throw new InvalidVisitStateException();
        }
        visitService.cancel(userId, visitId);
        visits.flush();
        VisitStartResponse started = visitService.start(userId,
                new VisitStartRequest(request.spotId(), request.startLatitude(), request.startLongitude()));
        visit.selectAlternativeVisit(started.visitId());
        return started;
    }

    private NudgeResponse existingOrBlocked(Visit visit, LocalDateTime now) {
        if (visit.getStatus() != VisitStatus.STARTED) {
            return NudgeResponse.state("INACTIVE");
        }
        if (!visit.getUser().isAlternativeNotificationEnabled()) {
            return NudgeResponse.state("DISABLED");
        }
        if (visit.getAlternativeDismissedAt() != null) {
            return NudgeResponse.state("DISMISSED");
        }
        String evaluation = policy.evaluate(visit.getStartQuietScore(), visit.getStartQuietScoreObservedAt(),
                visit.getStartedAt(), visit.getSpot().getCurrentQuietScore(),
                visit.getSpot().getQuietScoreUpdatedAt(), now);
        if (visit.getAlternativeSuggestionJson() != null) {
            Proposal proposal = mapper.readValue(visit.getAlternativeSuggestionJson(), Proposal.class);
            if (!now.isBefore(proposal.expiresAt()) || !"TRIGGERED".equals(evaluation)
                    || !Objects.equals(proposal.sourceObservedAt(), visit.getSpot().getQuietScoreUpdatedAt())
                    || !Objects.equals(proposal.currentQuietScore(), visit.getSpot().getCurrentQuietScore())) {
                return NudgeResponse.state("EXPIRED");
            }
            return new NudgeResponse("OFFERED", proposal);
        }
        return "TRIGGERED".equals(evaluation) ? null : NudgeResponse.state(evaluation);
    }

    private Visit ownedLocked(Long userId, Long visitId) {
        users.findByIdForUpdate(userId).orElseThrow(UnauthorizedException::new);
        Visit visit = visits.findById(visitId).orElseThrow(VisitNotFoundException::new);
        if (!visit.getUser().getId().equals(userId)) {
            throw new VisitNotFoundException();
        }
        return visit;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record Plan(Long spotId, Integer currentScore, LocalDateTime observedAt, NudgeResponse response) {
    }
}
