package com.coltrip.backend.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.visit.dto.VisitCompleteResponse;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 반경 판정이 클라이언트로 이전된 뒤(이슈 #113), 서버는 사용자 위치를 받지 않고
// 본인 소유의 STARTED 방문인지만 확인해 완료 처리한다.
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
    void completesOwnedStartedVisitWithoutAnyLocation() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        VisitCompleteResponse response = visitService.complete(USER_ID, VISIT_ID);

        assertEquals("COMPLETED", response.status());
    }

    @Test
    void rejectsWhenVisitBelongsToAnotherUser() {
        Visit visit = newStartedVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(VisitNotFoundException.class,
                () -> visitService.complete(USER_ID + 1, VISIT_ID));
    }

    @Test
    void rejectsWhenVisitAlreadyCompleted() {
        Visit visit = newStartedVisit();
        visit.markArrived();
        visit.complete();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        assertThrows(InvalidVisitStateException.class,
                () -> visitService.complete(USER_ID, VISIT_ID));
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
