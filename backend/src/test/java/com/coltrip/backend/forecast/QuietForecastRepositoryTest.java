package com.coltrip.backend.forecast;

import static org.junit.jupiter.api.Assertions.*;
import com.coltrip.backend.domain.forecast.*;
import com.coltrip.backend.domain.spot.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "spring.test.database.replace=NONE",
        "spring.datasource.url=jdbc:h2:mem:forecast;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QuietForecastRepositoryTest {
    @Autowired QuietForecastRepository forecasts;
    @Autowired TouristSpotRepository spots;
    @Autowired EntityManager em;
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 0);
    private final LocalDateTime target = now.plusHours(1);

    @Test void latestVersionAndSourceAreSelected() {
        var spot = spot("123", Category.PARK);
        save(spot, now.minusHours(2), now.plusHours(2), "70", "coltrip-ai");
        save(spot, now.minusHours(1), now.plusHours(2), "80", "coltrip-ai");
        save(spot, now, now.plusHours(2), "99", "other-source");
        var result = candidates(null, null);
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("80").compareTo(result.getFirst().getQuietIndex()));
    }

    @Test void expiredLatestDoesNotReviveOlderVersion() {
        var spot = spot("123", Category.PARK);
        save(spot, now.minusHours(3), now.plusHours(2), "80", "coltrip-ai");
        save(spot, now.minusHours(2), now.minusHours(1), "70", "coltrip-ai");
        assertTrue(candidates(null, null).isEmpty());
        assertTrue(forecasts.findTimeline(spot.getId(), target, target.plusHours(24), "coltrip-ai", now).isEmpty());
    }

    @Test void filtersCategoryAndModeWhileFetchingAllModes() {
        var park = spot("123", Category.PARK);
        em.persist(SpotMode.builder().spot(park).mode(Mode.NATURAL).build());
        em.persist(SpotMode.builder().spot(park).mode(Mode.SENSORY).build());
        var cafe = spot("456", Category.CAFE);
        save(park, now, now.plusHours(2), "80", "coltrip-ai");
        save(cafe, now, now.plusHours(2), "90", "coltrip-ai");
        em.flush();
        em.clear();
        var result = candidates(Category.PARK, Mode.NATURAL);
        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().getSpot().getModes().size());
        assertTrue(candidates(Category.PARK, Mode.TRANQUIL).isEmpty());
    }

    @Test void timelineUsesSameForecastVersionAndExactTargetSlot() {
        var spot = spot("123", Category.PARK);
        save(spot, now.minusHours(1), now.plusHours(2), "80", "coltrip-ai");
        var timeline = forecasts.findTimeline(spot.getId(), target, target.plusHours(24), "coltrip-ai", now);
        assertEquals(1, timeline.size());
        assertEquals(candidates(null, null).getFirst().getId(), timeline.getFirst().getId());
        assertTrue(forecasts.findCandidates(target.plusHours(1), "coltrip-ai", now,
                bd("34"), bd("36"), bd("128"), bd("130"), null, null).isEmpty());
    }

    private List<QuietForecast> candidates(Category category, Mode mode) {
        return forecasts.findCandidates(target, "coltrip-ai", now, bd("34"), bd("36"), bd("128"), bd("130"), category, mode);
    }
    private TouristSpot spot(String contentId, Category category) {
        return spots.save(TouristSpot.builder().tourApiContentId(contentId).name("Test").address("Busan")
                .latitude(bd("35")).longitude(bd("129")).category(category).build());
    }
    private void save(TouristSpot spot, LocalDateTime generated, LocalDateTime validUntil, String score, String source) {
        var f = new QuietForecast(spot, target, generated, source);
        f.correct(bd(score), validUntil, now, "test-model");
        forecasts.saveAndFlush(f);
    }
    private BigDecimal bd(String value) { return new BigDecimal(value); }
}
