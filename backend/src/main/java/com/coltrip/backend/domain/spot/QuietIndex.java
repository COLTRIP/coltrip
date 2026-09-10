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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 배치 계산 결과 이력. docs/schema.md 참고
// 같은 장소·같은 계산 시각의 재전송은 새 이력이 아니라 정정으로 취급하므로 (spot_id, calculated_at) 조합은 유일해야 한다.
@Entity
@Table(name = "quiet_index",
        uniqueConstraints = @UniqueConstraint(name = "uk_quiet_index_spot_calculated_at",
                columnNames = {"spot_id", "calculated_at"}))
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

    // 같은 장소·같은 계산 시각 데이터의 재전송(정정)을 새 이력으로 만들지 않고 기존 이력을 갱신한다.
    public void correct(Integer quietScore, String rawMetrics) {
        this.quietScore = quietScore;
        this.rawMetrics = rawMetrics;
    }
}
