package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.alternative.service.AlternativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// NudgeService.check()와 같은 순서: AI 호출 동안은 DB 트랜잭션/행 잠금을 들고 있지 않는다.
@Component
@RequiredArgsConstructor
public class BackgroundNudgeEvaluator {

    private final NudgeStore store;
    private final AlternativeService alternatives;
    private final BackgroundPushNotifier notifier;

    public void evaluate(Long userId, Long visitId) {
        NudgeStore.Plan plan = store.prepare(userId, visitId);
        NudgeResponse response = plan.response();
        if (response == null) {
            var result = alternatives.find(userId, plan.spotId());
            response = store.publish(userId, visitId, plan, result);
        }
        if ("OFFERED".equals(response.state())) {
            notifier.notifyIfNeeded(visitId, response.proposal());
        }
    }
}
