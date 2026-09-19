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

    // 주간(일 단위) API용 검증. hour는 AI가 하루 1값(고정 시각)만 제공하는 현재 관례에서
    // 그 날의 대표 슬롯일 뿐 실시간 의미가 없다(이슈 #134 피드백). 그래서 target()처럼
    // "이 시각이 이미 지났는지"가 아니라 "이 날짜가 이미 지났는지"만 날짜 단위로 검증한다 -
    // 오늘 날짜인데 대표 시각(예: 15시)이 현재 시각보다 이르다는 이유로 거부하지 않는다.
    public LocalDateTime weeklyTarget(LocalDate date, Integer hour, LocalDateTime now) {
        if (date == null || hour == null || hour < 0 || hour > 23) {
            throw new InvalidForecastRequestException("date와 0~23 범위의 hour가 필요합니다.");
        }
        LocalDate today = now.toLocalDate();
        if (date.isBefore(today) || date.plusDays(6).isAfter(today.plusDays(7))) {
            throw new InvalidForecastRequestException("오늘부터 7일 뒤까지의 날짜만 조회할 수 있습니다.");
        }
        return date.atTime(hour, 0);
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
