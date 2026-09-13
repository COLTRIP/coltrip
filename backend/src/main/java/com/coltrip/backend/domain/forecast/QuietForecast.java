package com.coltrip.backend.domain.forecast;

import com.coltrip.backend.domain.spot.TouristSpot;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiet_forecast", uniqueConstraints = @UniqueConstraint(
        name = "uk_forecast_version", columnNames = {"spot_id", "target_at", "source", "generated_at"}),
        indexes = @Index(name = "idx_forecast_target_source", columnList = "target_at,source,generated_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuietForecast {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private TouristSpot spot;

    @Column(name = "target_at", nullable = false)
    private LocalDateTime targetAt;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "quiet_index", nullable = false, precision = 5, scale = 2)
    private BigDecimal quietIndex;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    public QuietForecast(TouristSpot spot, LocalDateTime targetAt, LocalDateTime generatedAt,
                         String source) {
        this.spot = spot;
        this.targetAt = targetAt;
        this.generatedAt = generatedAt;
        this.source = source;
    }

    public void correct(BigDecimal quietIndex, LocalDateTime validUntil,
                        LocalDateTime receivedAt, String modelVersion) {
        this.quietIndex = quietIndex;
        this.validUntil = validUntil;
        this.receivedAt = receivedAt;
        this.modelVersion = modelVersion;
    }
}
