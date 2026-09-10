package com.coltrip.backend.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.visit.dto.VisitCancelResponse;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 진행 중인 방문 취소(#35) 검증
@ExtendWith(MockitoExtension.class)
class VisitCancelTest {

    private static final Long USER_ID = 1L;
    private static final Long VISIT_ID = 10L;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VisitService visitService;

    @Test
    void cancelsStartedVisit() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        VisitCancelResponse response = visitService.cancel(USER_ID, VISIT_ID);

        assertEquals("CANCELED", response.status());
    }

    @Test
    void rejectsCancelForAnotherUsersVisit() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(VisitNotFoundException.class,
                () -> visitService.cancel(USER_ID + 1, VISIT_ID));
    }

    @Test
    void rejectsCancelForAlreadyCompletedVisit() {
        Visit visit = newStartedVisit();
        visit.markArrived();
        visit.complete();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(InvalidVisitStateException.class,
                () -> visitService.cancel(USER_ID, VISIT_ID));
    }

    @Test
    void rejectsRepeatedCancel() {
        Visit visit = newStartedVisit();
        visit.cancel();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(InvalidVisitStateException.class,
                () -> visitService.cancel(USER_ID, VISIT_ID));
    }

    @Test
    void rejectsCancelForUnknownVisit() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class,
                () -> visitService.cancel(USER_ID, VISIT_ID));
    }

    private Visit newStartedVisit() {
        User user = User.builder().googleSub("sub").email("user@coltrip.dev").build();
        setId(user, USER_ID);
        TouristSpot spot = TouristSpot.builder()
                .tourApiContentId("TEST-SPOT")
                .name("테스트 카페")
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.15))
                .longitude(BigDecimal.valueOf(129.06))
                .category(Category.CAFE)
                .build();

        return Visit.builder()
                .user(user)
                .spot(spot)
                .startLatitude(BigDecimal.valueOf(35.15))
                .startLongitude(BigDecimal.valueOf(129.06))
                .build();
    }

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
