package com.coltrip.backend.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.coltrip.backend.domain.review.Review;
import com.coltrip.backend.domain.review.ReviewRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.review.dto.ReviewResponse;
import com.coltrip.backend.review.dto.ReviewUpdateRequest;
import com.coltrip.backend.review.exception.ReviewNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 리뷰 수정(#33) 검증
@ExtendWith(MockitoExtension.class)
class ReviewUpdateTest {

    private static final Long OWNER_ID = 1L;
    private static final Long REVIEW_ID = 100L;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void ownerCanUpdateRatingAndContent() {
        Review review = newReview(OWNER_ID, 3, "그저 그랬어요");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.update(OWNER_ID, REVIEW_ID,
                new ReviewUpdateRequest(5, "다시 보니 좋았어요"));

        assertEquals(5, response.rating());
        assertEquals("다시 보니 좋았어요", response.content());
    }

    @Test
    void omittingContentClearsIt() {
        Review review = newReview(OWNER_ID, 4, "한줄평 있음");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.update(OWNER_ID, REVIEW_ID,
                new ReviewUpdateRequest(4, null));

        assertNull(response.content());
    }

    @Test
    void nonOwnerGetsNotFound() {
        Review review = newReview(OWNER_ID, 3, "리뷰");
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

        assertThrows(ReviewNotFoundException.class,
                () -> reviewService.update(OWNER_ID + 1, REVIEW_ID, new ReviewUpdateRequest(1, null)));
    }

    @Test
    void unknownReviewThrows() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
                () -> reviewService.update(OWNER_ID, REVIEW_ID, new ReviewUpdateRequest(3, null)));
    }

    private Review newReview(Long userId, int rating, String content) {
        User user = User.builder().googleSub("sub").email("user@coltrip.dev").build();
        setId(user, userId);
        TouristSpot spot = TouristSpot.builder()
                .tourApiContentId("TEST-SPOT")
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

        return Review.builder().visit(visit).rating(rating).content(content).build();
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
