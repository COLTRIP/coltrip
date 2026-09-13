package com.coltrip.backend.visit.nudge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.alternative.service.AlternativeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NudgeServiceTest {
    @Test void disabledDoesNotCallAi() {
        NudgeStore store = mock(NudgeStore.class);
        AlternativeService ai = mock(AlternativeService.class);
        when(store.prepare(1L, 10L)).thenReturn(new NudgeStore.Plan(2L, 30, null, NudgeResponse.state("DISABLED")));
        assertEquals("DISABLED", new NudgeService(store, ai).check(1L, 10L).state());
        verifyNoInteractions(ai);
    }

    @Test void failureDoesNotPublishOrConsumeProposal() {
        NudgeStore store = mock(NudgeStore.class);
        AlternativeService ai = mock(AlternativeService.class);
        when(store.prepare(1L, 10L)).thenReturn(new NudgeStore.Plan(2L, 30, null, null));
        when(ai.find(1L, 2L)).thenThrow(new AiIntegrationException(HttpStatus.GATEWAY_TIMEOUT, "AiTimeout", "timeout"));
        assertThrows(AiIntegrationException.class, () -> new NudgeService(store, ai).check(1L, 10L));
        verify(store, never()).publish(any(), any(), any(), any());
    }
}
