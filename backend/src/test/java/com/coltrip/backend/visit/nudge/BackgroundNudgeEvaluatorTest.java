package com.coltrip.backend.visit.nudge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.alternative.dto.AlternativeListResponse;
import com.coltrip.backend.alternative.service.AlternativeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class BackgroundNudgeEvaluatorTest {

    private final NudgeStore store = mock(NudgeStore.class);
    private final AlternativeService ai = mock(AlternativeService.class);
    private final BackgroundPushNotifier notifier = mock(BackgroundPushNotifier.class);
    private final BackgroundNudgeEvaluator evaluator = new BackgroundNudgeEvaluator(store, ai, notifier);

    @Test
    void alreadyBlockedStateSkipsAiAndNotifier() {
        when(store.prepare(1L, 10L)).thenReturn(new NudgeStore.Plan(2L, 80, null, NudgeResponse.state("DISABLED")));

        evaluator.evaluate(1L, 10L);

        verifyNoInteractions(ai, notifier);
    }

    @Test
    void newlyOfferedProposalNotifies() {
        when(store.prepare(1L, 10L)).thenReturn(new NudgeStore.Plan(2L, 30, null, null));
        var aiResult = new AlternativeListResponse(true, 30.0, List.of(), "ok");
        when(ai.find(1L, 2L)).thenReturn(aiResult);
        var proposal = new NudgeResponse.Proposal(10L, 2L, 80, 30, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10), List.of());
        when(store.publish(eq(1L), eq(10L), any(), eq(aiResult))).thenReturn(new NudgeResponse("OFFERED", proposal));

        evaluator.evaluate(1L, 10L);

        verify(notifier).notifyIfNeeded(10L, proposal);
    }

    @Test
    void nonOfferedPublishResultDoesNotNotify() {
        when(store.prepare(1L, 10L)).thenReturn(new NudgeStore.Plan(2L, 80, null, null));
        var aiResult = new AlternativeListResponse(false, 80.0, List.of(), "ok");
        when(ai.find(1L, 2L)).thenReturn(aiResult);
        when(store.publish(eq(1L), eq(10L), any(), eq(aiResult)))
                .thenReturn(NudgeResponse.state("AI_NOT_TRIGGERED"));

        evaluator.evaluate(1L, 10L);

        verifyNoInteractions(notifier);
    }
}
