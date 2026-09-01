package com.coltrip.backend.review.service;

import com.coltrip.backend.domain.review.Review;
import com.coltrip.backend.domain.review.ReviewRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.review.dto.ReviewCreateRequest;
import com.coltrip.backend.review.dto.ReviewListResponse;
import com.coltrip.backend.review.dto.ReviewResponse;
import com.coltrip.backend.review.exception.ReviewNotAllowedException;
import com.coltrip.backend.review.exception.ReviewNotFoundException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final VisitRepository visitRepository;

    public ReviewResponse create(Long userId, Long visitId, ReviewCreateRequest request) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(VisitNotFoundException::new);

        // 남의 방문 기록으로 리뷰를 쓰지 못하게. 존재 여부를 노출하지 않도록 404로 통일
        if (!visit.getUser().getId().equals(userId)) {
            throw new VisitNotFoundException();
        }
        if (visit.getStatus() != VisitStatus.COMPLETED) {
            throw new ReviewNotAllowedException("방문을 완료한 장소에만 리뷰를 작성할 수 있습니다.");
        }
        if (reviewRepository.existsByVisit_Id(visitId)) {
            throw new ReviewNotAllowedException("이미 이 방문에 대한 리뷰를 작성했습니다.");
        }

        Review review = reviewRepository.save(Review.builder()
                .visit(visit)
                .rating(request.rating())
                .content(request.content())
                .build());

        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse findBySpot(Long spotId) {
        return ReviewListResponse.from(reviewRepository.findBySpotIdWithUser(spotId));
    }

    public void delete(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        if (!review.isWrittenBy(userId)) {
            throw new ReviewNotFoundException();
        }

        reviewRepository.delete(review);
    }
}
