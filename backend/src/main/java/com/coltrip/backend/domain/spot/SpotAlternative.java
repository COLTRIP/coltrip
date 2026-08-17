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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 배치 저장 전제로 설계, 실시간 호출로 바뀌면 이 엔티티는 안 쓸 수 있음 - docs/schema.md 참고
@Entity
@Table(name = "spot_alternative")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotAlternative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_spot_id", nullable = false)
    private TouristSpot originSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternative_spot_id", nullable = false)
    private TouristSpot alternativeSpot;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal similarityScore;

    @Column(name = "recommend_reason", nullable = false, length = 500)
    private String recommendReason;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Builder
    public SpotAlternative(TouristSpot originSpot, TouristSpot alternativeSpot,
                            BigDecimal similarityScore, String recommendReason, LocalDateTime calculatedAt) {
        this.originSpot = originSpot;
        this.alternativeSpot = alternativeSpot;
        this.similarityScore = similarityScore;
        this.recommendReason = recommendReason;
        this.calculatedAt = calculatedAt;
    }
}
