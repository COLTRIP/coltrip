package com.coltrip.backend.forecast;

import com.coltrip.backend.domain.forecast.QuietForecast;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public final class ForecastResponses {
    private ForecastResponses() { }

    public record Point(String type, OffsetDateTime targetAt,
            @Schema(description = "예측 고요지수 0~100. 없으면 null. 현재 점수로 대체하지 않음") BigDecimal quietIndex,
            OffsetDateTime generatedAt, OffsetDateTime validUntil, String source, String modelVersion) {
        public static Point from(QuietForecast value) {
            return new Point("FORECAST", offset(value.getTargetAt()), value.getQuietIndex(),
                    offset(value.getGeneratedAt()), offset(value.getValidUntil()), value.getSource(), value.getModelVersion());
        }
        public static Point empty(LocalDateTime targetAt) {
            return new Point("FORECAST", offset(targetAt), null, null, null, null, null);
        }
    }

    public record Place(Long id, String name, String address, String category, List<String> modes,
            String imageUrl, BigDecimal latitude, BigDecimal longitude, boolean isLiked) { }

    public record Item(Place spot, long distanceMeters, Point forecast) { }

    public record Recommendations(String timezone, OffsetDateTime targetAt,
            BigDecimal latitude, BigDecimal longitude, int radiusMeters, boolean defaultCenter,
            String sort, List<Item> spots, String message) { }

    public record Timeline(String timezone, Long spotId, List<Point> timeline) { }

    public static OffsetDateTime offset(LocalDateTime value) {
        return value.atZone(ForecastPolicy.ZONE).toOffsetDateTime();
    }
}
