package com.coltrip.backend.visit.nudge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BackgroundNudgeSchedulerTest {

    private final VisitRepository visits = mock(VisitRepository.class);
    private final BackgroundNudgeEvaluator evaluator = mock(BackgroundNudgeEvaluator.class);
    private final BackgroundNudgeScheduler scheduler = new BackgroundNudgeScheduler(visits, evaluator);

    @Test
    void noCandidatesSkipsEvaluation() {
        when(visits.findForBackgroundNudge(VisitStatus.STARTED)).thenReturn(List.of());

        scheduler.evaluateActiveVisits();

        verifyNoInteractions(evaluator);
    }

    @Test
    void evaluatesEachCandidateByOwner() {
        User user1 = user(1L);
        User user2 = user(2L);
        Visit visit1 = visit(10L, user1);
        Visit visit2 = visit(20L, user2);
        when(visits.findForBackgroundNudge(VisitStatus.STARTED)).thenReturn(List.of(visit1, visit2));

        scheduler.evaluateActiveVisits();

        verify(evaluator).evaluate(1L, 10L);
        verify(evaluator).evaluate(2L, 20L);
    }

    @Test
    void oneVisitFailingDoesNotStopTheRest() {
        User user1 = user(1L);
        User user2 = user(2L);
        Visit visit1 = visit(10L, user1);
        Visit visit2 = visit(20L, user2);
        when(visits.findForBackgroundNudge(VisitStatus.STARTED)).thenReturn(List.of(visit1, visit2));
        doThrow(new RuntimeException("boom")).when(evaluator).evaluate(1L, 10L);

        assertDoesNotThrow(scheduler::evaluateActiveVisits);

        verify(evaluator).evaluate(2L, 20L);
    }

    private User user(Long id) {
        User user = User.builder().googleSub("g" + id).email(id + "@a.com").build();
        setId(user, id);
        return user;
    }

    private Visit visit(Long id, User user) {
        Visit visit = Visit.builder().user(user).spot(spot()).build();
        setId(visit, id);
        return visit;
    }

    private TouristSpot spot() {
        return TouristSpot.builder().tourApiContentId("1").name("n").address("a")
                .latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).category(Category.CAFE).build();
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
