package com.coltrip.backend.visit.nudge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.push.service.PushSender;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BackgroundPushNotifierTest {

    private final VisitRepository visits = mock(VisitRepository.class);
    private final PushSender pushSender = mock(PushSender.class);
    private final BackgroundPushNotifier notifier = new BackgroundPushNotifier(visits, pushSender);

    @Test
    void sendsAndMarksWhenNeverNotified() {
        User user = User.builder().googleSub("g").email("a@a.com").build();
        Visit visit = Visit.builder().user(user).spot(spot()).build();
        when(visits.findById(10L)).thenReturn(Optional.of(visit));
        var proposal = new NudgeResponse.Proposal(10L, 1L, 80, 30, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10), List.of());

        notifier.notifyIfNeeded(10L, proposal);

        verify(pushSender).sendAlternativeSuggestion(user, 10L);
    }

    @Test
    void skipsWhenAlreadyNotifiedForThisProposal() {
        User user = User.builder().googleSub("g").email("a@a.com").build();
        Visit visit = Visit.builder().user(user).spot(spot()).build();
        LocalDateTime issuedAt = LocalDateTime.now();
        visit.markAlternativeNotified(issuedAt);
        when(visits.findById(10L)).thenReturn(Optional.of(visit));
        var proposal = new NudgeResponse.Proposal(10L, 1L, 80, 30, LocalDateTime.now(), issuedAt,
                issuedAt.plusMinutes(10), List.of());

        notifier.notifyIfNeeded(10L, proposal);

        verifyNoInteractions(pushSender);
    }

    @Test
    void notifiesAgainForNewerProposalAfterExpiry() {
        User user = User.builder().googleSub("g").email("a@a.com").build();
        Visit visit = Visit.builder().user(user).spot(spot()).build();
        visit.markAlternativeNotified(LocalDateTime.now().minusHours(1));
        when(visits.findById(10L)).thenReturn(Optional.of(visit));
        var newerProposal = new NudgeResponse.Proposal(10L, 1L, 80, 30, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10), List.of());

        notifier.notifyIfNeeded(10L, newerProposal);

        verify(pushSender).sendAlternativeSuggestion(user, 10L);
    }

    private TouristSpot spot() {
        return TouristSpot.builder().tourApiContentId("1").name("n").address("a")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).category(Category.CAFE).build();
    }
}
