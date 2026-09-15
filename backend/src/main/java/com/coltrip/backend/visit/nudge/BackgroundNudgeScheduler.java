package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// 진행 중(STARTED) 방문을 주기적으로 훑어 고요지수 하락을 평가하고, 새로 제안이 생기면 푸시로 알린다.
// 프론트가 앱을 띄워 /check를 직접 호출하는 기존 흐름과 별개로, 백그라운드에서도 같은 판정이 이뤄지게 한다.
// 평가 주기(2분)는 잠정값 - 운영 데이터로 AI 호출 비용과 알림 지연을 보고 조정 필요(이슈 #89).
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "demo.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class BackgroundNudgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackgroundNudgeScheduler.class);

    private final VisitRepository visits;
    private final BackgroundNudgeEvaluator evaluator;

    @Scheduled(fixedRateString = "${visit.nudge.background-interval-ms:120000}")
    public void evaluateActiveVisits() {
        var candidates = visits.findForBackgroundNudge(VisitStatus.STARTED);
        int failed = 0;
        for (Visit visit : candidates) {
            try {
                evaluator.evaluate(visit.getUser().getId(), visit.getId());
            } catch (RuntimeException e) {
                failed++;
                log.warn("백그라운드 고요지수 평가 실패. visitId={}", visit.getId(), e);
            }
        }
        if (!candidates.isEmpty()) {
            log.info("백그라운드 고요지수 평가 완료. 대상 {}건 중 {}건 실패", candidates.size(), failed);
        }
    }
}
