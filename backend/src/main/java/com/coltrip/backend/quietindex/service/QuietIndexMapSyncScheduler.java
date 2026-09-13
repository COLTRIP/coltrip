package com.coltrip.backend.quietindex.service;

import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.quietindex.client.AiQuietIndexMapClient;
import com.coltrip.backend.quietindex.client.AiQuietIndexMapClient.Item;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// AI GET /quiet-index/map을 1시간 주기로 당겨와 우리 DB에 있는 장소만 반영한다(이슈 #63).
// 지도 요청마다 AI를 부르지 않고, 이 스케줄러가 당긴 값을 tourist_spot 캐시가 대신 서빙한다.
// AI 호출 실패 시 이번 주기는 건너뛰고 마지막 정상값을 그대로 유지한다.
@Service
public class QuietIndexMapSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuietIndexMapSyncScheduler.class);

    private final AiQuietIndexMapClient client;
    private final TouristSpotRepository spots;
    private final QuietIndexMapApplier applier;
    private final Clock clock;

    public QuietIndexMapSyncScheduler(AiQuietIndexMapClient client, TouristSpotRepository spots,
                                      QuietIndexMapApplier applier,
                                      @Qualifier("alternativeClock") Clock clock) {
        this.client = client;
        this.spots = spots;
        this.applier = applier;
        this.clock = clock;
    }

    @Scheduled(cron = "${quiet-index.map-sync.cron:0 5 * * * *}")
    public void sync() {
        Set<String> ours = new HashSet<>(spots.findAllTourApiContentIds());
        if (ours.isEmpty()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(clock);
        boolean weekend = now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY;

        List<Item> items;
        try {
            items = client.fetchMap(now.getHour(), weekend);
        } catch (RuntimeException e) {
            log.warn("AI 고요지수 지도 동기화 실패, 마지막 정상값 유지: {}", e.getMessage());
            return;
        }

        LocalDateTime calculatedAt = now.toLocalDateTime();
        int matched = 0;
        for (Item item : items) {
            if (item.quietIndex() == null || !ours.contains(item.poiId())) {
                continue;
            }
            if (applier.apply(item.poiId(), item.quietIndex(), calculatedAt)) {
                matched++;
            }
        }
        log.info("AI 고요지수 지도 동기화 완료: 응답 {}건 중 {}건 반영", items.size(), matched);
    }
}
