package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.push.service.PushSender;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import com.coltrip.backend.visit.nudge.NudgeResponse.Proposal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// BackgroundNudgeEvaluator와 별도 빈으로 둔다 - 같은 빈 안에서 호출하면
// @Transactional 프록시가 적용되지 않는다(자기 자신 호출 문제).
@Component
@RequiredArgsConstructor
public class BackgroundPushNotifier {

    private final VisitRepository visits;
    private final PushSender pushSender;

    @Transactional
    public void notifyIfNeeded(Long visitId, Proposal proposal) {
        Visit visit = visits.findById(visitId).orElseThrow(VisitNotFoundException::new);
        if (!visit.needsAlternativeNotification(proposal.issuedAt())) {
            return;
        }
        pushSender.sendAlternativeSuggestion(visit.getUser(), visitId);
        visit.markAlternativeNotified(proposal.issuedAt());
    }
}
