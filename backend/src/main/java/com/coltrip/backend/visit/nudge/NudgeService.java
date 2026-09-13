package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.alternative.service.AlternativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NudgeService {
    private final NudgeStore store;
    private final AlternativeService alternatives;

    public NudgeResponse check(Long userId, Long visitId) {
        NudgeStore.Plan plan = store.prepare(userId, visitId);
        if (plan.response() != null) {
            return plan.response();
        }
        // HTTP 호출 중에는 DB 트랜잭션과 사용자 행 잠금을 유지하지 않는다.
        var result = alternatives.find(userId, plan.spotId());
        return store.publish(userId, visitId, plan, result);
    }
}
