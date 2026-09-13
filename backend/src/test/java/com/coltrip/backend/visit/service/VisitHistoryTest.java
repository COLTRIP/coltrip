package com.coltrip.backend.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.coltrip.backend.domain.review.Review;
import com.coltrip.backend.domain.review.ReviewRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.visit.dto.VisitHistoryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 방문 완료 이력 조회(#39) 검증
@ExtendWith(MockitoExtension.class)
class VisitHistoryTest {

    private static final Long USER_ID = 1L;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private VisitService visitService;

    @Test
    void returnsEmptyWhenNoCompletedVisits() {
        when(visitRepository.findByUserIdAndStatusOrderByCompletedAtDesc(USER_ID, VisitStatus.COMPLETED))
                .thenReturn(List.of());

        VisitHistoryResponse response = visitService.getHistory(USER_ID);

        assertTrue(response.visits().isEmpty());
    }

    @Test
    void marksReviewIdOnlyForReviewedVisits() {
        Visit reviewedVisit = completedVisit(10L);
        Visit unreviewedVisit = completedVisit(11L);
        when(visitRepository.findByUserIdAndStatusOrderByCompletedAtDesc(USER_ID, VisitStatus.COMPLETED))
                .thenReturn(List.of(unreviewedVisit, reviewedVisit));

        Review review = Review.builder().visit(reviewedVisit).rating(5).content("좋았어요").build();
        setId(review, 99L);
        when(reviewRepository.findByVisit_IdIn(List.of(11L, 10L))).thenReturn(List.of(review));

        VisitHistoryResponse response = visitService.getHistory(USER_ID);

        var byId = response.visits().stream()
                .collect(java.util.stream.Collectors.toMap(VisitHistoryResponse.VisitHistoryItem::visitId, v -> v));
        assertEquals(99L, byId.get(10L).reviewId());
        assertNull(byId.get(11L).reviewId());
    }

    private Visit completedVisit(Long visitId) {
        User user = User.builder().googleSub("sub").email("user@coltrip.dev").build();
        TouristSpot spot = TouristSpot.builder()
                .tourApiContentId("TEST-SPOT-" + visitId)
                .name("테스트 장소")
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.1))
                .longitude(BigDecimal.valueOf(129.1))
                .category(Category.CAFE)
                .build();
        Visit visit = Visit.builder()
                .user(user)
                .spot(spot)
                .startLatitude(BigDecimal.valueOf(35.1))
                .startLongitude(BigDecimal.valueOf(129.1))
                .build();
        visit.markArrived();
        visit.complete();
        setId(visit, visitId);
        return visit;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
