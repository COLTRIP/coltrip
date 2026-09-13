package com.coltrip.backend.forecast;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ForecastPolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public LocalDateTime target(LocalDate date, Integer hour, LocalDateTime now) {
        if (date == null || hour == null || hour < 0 || hour > 23) {
            throw new InvalidForecastRequestException("date와 0~23 범위의 hour가 필요합니다.");
        }
        LocalDateTime value = date.atTime(hour, 0);
        LocalDateTime first = now.truncatedTo(ChronoUnit.HOURS);
        if (value.isBefore(first) || value.isAfter(first.plusDays(7))) {
            throw new InvalidForecastRequestException("현재 시간 슬롯부터 7일 뒤 같은 시간 슬롯까지 조회할 수 있습니다.");
        }
        return value;
    }

    public Area area(BigDecimal latitude, BigDecimal longitude, int radiusMeters, int limit) {
        if ((latitude == null) != (longitude == null)) {
            throw new InvalidForecastRequestException("latitude와 longitude는 함께 입력해야 합니다.");
        }
        if (radiusMeters < 100 || radiusMeters > 50000 || limit < 1 || limit > 50) {
            throw new InvalidForecastRequestException("radiusMeters는 100~50000, limit은 1~50이어야 합니다.");
        }
        boolean defaultCenter = latitude == null;
        BigDecimal lat = defaultCenter ? new BigDecimal("35.1796") : latitude;
        BigDecimal lng = defaultCenter ? new BigDecimal("129.0756") : longitude;
        if (lat.abs().compareTo(BigDecimal.valueOf(90)) > 0 || lng.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new InvalidForecastRequestException("위도 또는 경도 범위가 올바르지 않습니다.");
        }
        double angular = radiusMeters / 6371000.0;
        double latitudeDelta = Math.toDegrees(angular);
        double south = Math.max(-90, lat.doubleValue() - latitudeDelta);
        double north = Math.min(90, lat.doubleValue() + latitudeDelta);
        double west = -180;
        double east = 180;
        if (south > -90 && north < 90) {
            double delta = Math.toDegrees(Math.asin(Math.min(1,
                    Math.sin(angular) / Math.cos(Math.toRadians(lat.doubleValue())))));
            if (lng.doubleValue() - delta >= -180 && lng.doubleValue() + delta <= 180) {
                west = lng.doubleValue() - delta;
                east = lng.doubleValue() + delta;
            }
        }
        return new Area(lat, lng, radiusMeters, defaultCenter,
                BigDecimal.valueOf(south), BigDecimal.valueOf(north), BigDecimal.valueOf(west), BigDecimal.valueOf(east));
    }

    public LocalDateTime local(OffsetDateTime value) {
        return value.atZoneSameInstant(ZONE).toLocalDateTime().truncatedTo(ChronoUnit.MICROS);
    }

    public record Area(BigDecimal latitude, BigDecimal longitude, int radiusMeters, boolean defaultCenter,
                       BigDecimal south, BigDecimal north, BigDecimal west, BigDecimal east) {
    }
}
