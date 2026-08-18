package com.coltrip.backend.domain.spot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tourist_spot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TouristSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tour_api_content_id", nullable = false, unique = true, length = 50)
    private String tourApiContentId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Lob
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "recommend_reason", length = 500)
    private String recommendReason;

    @Column(name = "current_quiet_score")
    private Integer currentQuietScore;

    @Column(name = "quiet_score_updated_at")
    private LocalDateTime quietScoreUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TouristSpot(String tourApiContentId, String name, String address,
                        BigDecimal latitude, BigDecimal longitude, Category category,
                        String description, String imageUrl, String recommendReason) {
        this.tourApiContentId = tourApiContentId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.recommendReason = recommendReason;
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

    public void updateQuietScore(int quietScore, LocalDateTime calculatedAt) {
        this.currentQuietScore = quietScore;
        this.quietScoreUpdatedAt = calculatedAt;
    }

    public QuietLevel getQuietLevel() {
        return QuietLevel.from(currentQuietScore);
    }
}
