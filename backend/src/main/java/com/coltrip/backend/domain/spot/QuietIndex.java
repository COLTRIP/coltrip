package com.coltrip.backend.domain.spot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 배치 계산 결과 이력. docs/schema.md 참고
@Entity
@Table(name = "quiet_index")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuietIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private TouristSpot spot;

    @Column(name = "quiet_score", nullable = false)
    private Integer quietScore;

    @Column(name = "raw_metrics", columnDefinition = "json")
    private String rawMetrics;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Builder
    public QuietIndex(TouristSpot spot, Integer quietScore, String rawMetrics, LocalDateTime calculatedAt) {
        this.spot = spot;
        this.quietScore = quietScore;
        this.rawMetrics = rawMetrics;
        this.calculatedAt = calculatedAt;
    }
}
