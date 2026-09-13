package com.coltrip.backend.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.visit.dto.VisitCompleteRequest;
import com.coltrip.backend.visit.dto.VisitCompleteResponse;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitConditionNotMetException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 체류시간 조건 제거 후 반경 진입만으로 방문 완료를 판정하는지 검증한다.
@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long VISIT_ID = 10L;
    private static final BigDecimal SPOT_LAT = BigDecimal.valueOf(35.15);
    private static final BigDecimal SPOT_LNG = BigDecimal.valueOf(129.06);

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VisitService visitService;

    @BeforeEach
    void allowUserLock() {
        when(userRepository.findByIdForUpdate(anyLong()))
                .thenReturn(Optional.of(User.builder()
                        .googleSub("lock-user")
                        .email("lock@example.com")
                        .build()));
    }

    @Test
    void completesWhenArrivedExactlyAtSpot() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        VisitCompleteResponse response = visitService.complete(USER_ID, VISIT_ID,
                new VisitCompleteRequest(SPOT_LAT, SPOT_LNG));

        assertEquals("COMPLETED", response.status());
    }

    @Test
    void rejectsWhenFarOutsideRadius() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        // 카페(반경 100m) 기준으로 약 1.1km 떨어진 좌표
        VisitCompleteRequest farAway = new VisitCompleteRequest(
                SPOT_LAT.add(BigDecimal.valueOf(0.01)), SPOT_LNG);

        assertThrows(VisitConditionNotMetException.class,
                () -> visitService.complete(USER_ID, VISIT_ID, farAway));
    }

    @Test
    void rejectsWhenVisitBelongsToAnotherUser() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(VisitNotFoundException.class,
                () -> visitService.complete(USER_ID + 1, VISIT_ID, new VisitCompleteRequest(SPOT_LAT, SPOT_LNG)));
    }

    @Test
    void rejectsWhenVisitAlreadyCompleted() {
        Visit visit = newStartedVisit();
        visit.markArrived();
        visit.complete();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(InvalidVisitStateException.class,
                () -> visitService.complete(USER_ID, VISIT_ID, new VisitCompleteRequest(SPOT_LAT, SPOT_LNG)));
    }

    private Visit newStartedVisit() {
        User user = User.builder().googleSub("sub").email("user@coltrip.dev").build();
        setId(user, USER_ID);
        TouristSpot spot = TouristSpot.builder()
                .tourApiContentId("TEST-SPOT")
                .name("테스트 카페")
                .address("부산 테스트 주소")
                .latitude(SPOT_LAT)
                .longitude(SPOT_LNG)
                .category(Category.CAFE)
                .build();

        return Visit.builder()
                .user(user)
                .spot(spot)
                .startLatitude(SPOT_LAT)
                .startLongitude(SPOT_LNG)
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
