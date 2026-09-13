package com.coltrip.backend.visit.nudge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.coltrip.backend.alternative.dto.AlternativeListResponse;
import com.coltrip.backend.alternative.dto.AlternativeListResponse.Alternative;
import com.coltrip.backend.alternative.dto.AlternativeListResponse.Place;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import com.coltrip.backend.visit.service.VisitService;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class NudgeStoreTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 0);
    private final Clock clock = Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private VisitRepository visits;
    private UserRepository users;
    private VisitService visitService;
    private User user;
    private TouristSpot spot;
    private Visit visit;
    private NudgeStore store;

    @BeforeEach void setUp() throws Exception {
        visits = mock(VisitRepository.class);
        users = mock(UserRepository.class);
        visitService = mock(VisitService.class);
        user = User.builder().googleSub("sub").email("test@example.com").build();
        set(user, "id", 1L);
        spot = TouristSpot.builder().tourApiContentId("123").name("Place").address("Busan")
                .latitude(BigDecimal.valueOf(35)).longitude(BigDecimal.valueOf(129)).category(Category.PARK).build();
        set(spot, "id", 2L);
        spot.updateQuietScoreIfNewer(70, now.minusHours(1));
        visit = Visit.builder().user(user).spot(spot).startLatitude(spot.getLatitude()).startLongitude(spot.getLongitude()).build();
        set(visit, "id", 10L);
        set(visit, "startedAt", now.minusMinutes(30));
        spot.updateQuietScoreIfNewer(30, now);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(visits.findById(10L)).thenReturn(Optional.of(visit));
        store = new NudgeStore(visits, users, visitService,
                new NudgePolicy(Duration.ofHours(5), Duration.ofMinutes(10)), new ObjectMapper(), clock);
    }

    @Test void snapshotsSurviveRetryWithSameId() {
        var plan = store.prepare(1L, 10L);
        assertNull(plan.response());
        var first = store.publish(1L, 10L, plan, result());
        assertEquals("OFFERED", first.state());
        assertEquals(first, store.prepare(1L, 10L).response());
        assertEquals(first, store.publish(1L, 10L, plan, result()));
    }

    @Test void completedWhileAiWasRunningDoesNotPublish() {
        var plan = store.prepare(1L, 10L);
        visit.complete();
        assertEquals("INACTIVE", store.publish(1L, 10L, plan, result()).state());
        assertNull(visit.getAlternativeSuggestionJson());
    }

    @Test void disabledWhileAiWasRunningDoesNotPublish() {
        var plan = store.prepare(1L, 10L);
        user.updateAlternativeNotificationEnabled(false);
        assertEquals("DISABLED", store.publish(1L, 10L, plan, result()).state());
        assertNull(visit.getAlternativeSuggestionJson());
    }

    @Test void changedScoreInvalidatesPendingResult() {
        var plan = store.prepare(1L, 10L);
        spot.updateQuietScoreIfNewer(31, now);
        assertEquals("SCORE_CHANGED", store.publish(1L, 10L, plan, result()).state());
    }

    @Test void emptyDoesNotConsumeProposal() {
        var plan = store.prepare(1L, 10L);
        assertEquals("NO_ALTERNATIVES", store.publish(1L, 10L, plan,
                new AlternativeListResponse(true, 30, List.of(), "none")).state());
        assertNull(visit.getAlternativeSuggestionJson());
    }

    @Test void dismissalPersistsAndPreventsSelection() {
        store.publish(1L, 10L, store.prepare(1L, 10L), result());
        store.dismiss(1L, 10L);
        assertEquals("DISMISSED", store.prepare(1L, 10L).response().state());
        assertThrows(InvalidVisitStateException.class, () -> store.select(1L, 10L, request(3L)));
    }

    @Test void changedScoreExpiresExistingProposal() {
        store.publish(1L, 10L, store.prepare(1L, 10L), result());
        spot.updateQuietScoreIfNewer(31, now);
        assertEquals("EXPIRED", store.prepare(1L, 10L).response().state());
    }

    @Test void exactExpiryBoundaryIsExpired() {
        store.publish(1L, 10L, store.prepare(1L, 10L), result());
        NudgeStore later = new NudgeStore(visits, users, visitService,
                new NudgePolicy(Duration.ofHours(5), Duration.ofMinutes(10)), new ObjectMapper(),
                Clock.offset(clock, Duration.ofMinutes(10)));
        assertEquals("EXPIRED", later.prepare(1L, 10L).response().state());
    }

    @Test void selectionRejectsUnlistedPlace() {
        store.publish(1L, 10L, store.prepare(1L, 10L), result());
        assertThrows(InvalidVisitStateException.class, () -> store.select(1L, 10L, request(999L)));
        verifyNoInteractions(visitService);
    }

    @Test void selectionRetryDoesNotStartAgain() throws Exception {
        store.publish(1L, 10L, store.prepare(1L, 10L), result());
        when(visitService.start(eq(1L), any())).thenReturn(new VisitStartResponse(11L, "STARTED", now));
        assertEquals(11L, store.select(1L, 10L, request(3L)).visitId());
        TouristSpot selectedSpot = mock(TouristSpot.class);
        when(selectedSpot.getId()).thenReturn(3L);
        Visit selected = Visit.builder().user(user).spot(selectedSpot).build();
        set(selected, "id", 11L);
        when(visits.findById(11L)).thenReturn(Optional.of(selected));
        assertEquals(11L, store.select(1L, 10L, request(3L)).visitId());
        verify(visitService, times(1)).cancel(1L, 10L);
        verify(visitService, times(1)).start(eq(1L), any());
    }

    @Test void anotherUsersVisitIsHidden() throws Exception {
        User other = User.builder().googleSub("other").email("other@example.com").build();
        set(other, "id", 9L);
        set(visit, "user", other);
        assertThrows(VisitNotFoundException.class, () -> store.prepare(1L, 10L));
    }

    private AlternativeListResponse result() {
        Place place = new Place(3L, "Alternative", "Busan", "PARK", List.of(), null,
                BigDecimal.valueOf(35.01), BigDecimal.valueOf(129), false);
        return new AlternativeListResponse(true, 30, List.of(new Alternative(place, 70.5, 1.1, .8, "Quieter")), "ok");
    }
    private NudgeSelectRequest request(Long spotId) {
        return new NudgeSelectRequest(spotId, BigDecimal.valueOf(35), BigDecimal.valueOf(129));
    }
    private void set(Object object, String name, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
