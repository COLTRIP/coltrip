package com.coltrip.backend.domain.review;

import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.Visit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 별점(1~5) + 한줄평. 방문 완료(Visit.COMPLETED)한 사용자만 작성 가능, visit 1건당 리뷰 1건.
@Entity
@Table(name = "review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성 자격의 근거. unique 제약으로 "방문 1건당 리뷰 1건"을 DB 레벨에서 보장
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false, unique = true)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private TouristSpot spot;

    // 1~5 별점
    @Column(nullable = false)
    private Integer rating;

    @Column(length = 300)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Review(Visit visit, Integer rating, String content) {
        this.visit = visit;
        this.user = visit.getUser();
        this.spot = visit.getSpot();
        this.rating = rating;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    public boolean isWrittenBy(Long userId) {
        return user.getId().equals(userId);
    }
}
