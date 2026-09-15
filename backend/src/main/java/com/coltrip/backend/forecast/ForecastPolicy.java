package com.coltrip.backend.forecast;

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

    public void validateLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new InvalidForecastRequestException("limit은 1~50이어야 합니다.");
        }
    }

    public LocalDateTime local(OffsetDateTime value) {
        return value.atZoneSameInstant(ZONE).toLocalDateTime().truncatedTo(ChronoUnit.MICROS);
    }
}
