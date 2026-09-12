package com.coltrip.backend.domain.visit;

import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private TouristSpot spot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    @Column(name = "start_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal startLatitude;

    @Column(name = "start_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal startLongitude;

    @Column(name = "start_quiet_score")
    private Integer startQuietScore;

    @Column(name = "start_quiet_score_observed_at")
    private LocalDateTime startQuietScoreObservedAt;

    @Column(name = "alternative_suggestion_json", columnDefinition = "LONGTEXT")
    private String alternativeSuggestionJson;

    @Column(name = "alternative_dismissed_at")
    private LocalDateTime alternativeDismissedAt;

    @Column(name = "alternative_selected_visit_id")
    private Long alternativeSelectedVisitId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public Visit(User user, TouristSpot spot, BigDecimal startLatitude, BigDecimal startLongitude) {
        this.user = user;
        this.spot = spot;
        this.status = VisitStatus.STARTED;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.startQuietScore = spot.getCurrentQuietScore();
        this.startQuietScoreObservedAt = spot.getQuietScoreUpdatedAt();
    }

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
    }

    public void markArrived() {
        this.arrivedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = VisitStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = VisitStatus.CANCELED;
    }

    public void saveAlternativeSuggestion(String json) {
        this.alternativeSuggestionJson = json;
    }

    public void dismissAlternativeSuggestion(LocalDateTime dismissedAt) {
        this.alternativeDismissedAt = dismissedAt;
    }

    public void selectAlternativeVisit(Long visitId) {
        this.alternativeSelectedVisitId = visitId;
    }
}
