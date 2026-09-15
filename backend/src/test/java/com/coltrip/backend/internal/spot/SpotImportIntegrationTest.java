package com.coltrip.backend.internal.spot;

import static org.junit.jupiter.api.Assertions.*;
import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.spot.*;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.like.SpotLike;
import com.coltrip.backend.domain.review.Review;
import com.coltrip.backend.internal.dto.QuietIndexPushRequest;
import com.coltrip.backend.internal.service.InternalQuietIndexService;
import com.coltrip.backend.spot.service.SpotService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.test.database.replace=NONE",
        "spring.datasource.url=jdbc:h2:mem:spotimport;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({SpotImportService.class, InternalQuietIndexService.class, SpotService.class, SpotImportIntegrationTest.Config.class})
class SpotImportIntegrationTest {
    @Autowired SpotImportService imports;
    @Autowired TouristSpotRepository spots;
    @Autowired SpotModeRepository modes;
    @Autowired QuietIndexRepository quietIndices;
    @Autowired InternalQuietIndexService quiet;
    @Autowired SpotService reads;
    @Autowired EntityManager em;
    private final OffsetDateTime source = OffsetDateTime.parse("2026-09-12T12:00:00+09:00");

    @TestConfiguration
    static class Config {
        @Bean InternalApiProperties internalApiProperties() { return new InternalApiProperties("test-key"); }
    }

    @Test void allEightEmotionsSurvivePersistenceAndResponseMapping() {
        Long id = imports.receive("test-key", request("All emotions", List.of(Mode.values()), source, null))
                .spots().getFirst().spotId();
        clear();
        assertEquals(java.util.Set.of(Mode.values()), modes.findBySpot_Id(id).stream()
                .map(SpotMode::getMode).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Arrays.stream(Mode.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                new java.util.HashSet<>(reads.findById(null, id).modes()));
    }

    @Test void createsSpotAndDeduplicatesModes() {
        var result = imports.receive("test-key", request("First", List.of(Mode.NATURAL, Mode.NATURAL, Mode.SENSORY), source, "https://example.com/a.jpg"));
        clear();
        assertEquals(1, result.applied());
        assertEquals(1, spots.count());
        var spot = spots.findByTourApiContentId("123").orElseThrow();
        assertEquals(2, modes.findBySpot_Id(spot.getId()).size());
        assertNull(spot.getCurrentQuietScore());
        assertEquals(source.toLocalDateTime(), spot.getSourceUpdatedAt());
    }

    @Test void replayKeepsSpotAndMappingIds() {
        var first = imports.receive("test-key", request("First", List.of(Mode.NATURAL), source, null));
        clear();
        Long id = first.spots().getFirst().spotId();
        Long mappingId = modes.findBySpot_Id(id).getFirst().getId();
        imports.receive("test-key", request("First", List.of(Mode.NATURAL), source, null));
        clear();
        assertEquals(1, spots.count());
        assertEquals(id, spots.findByTourApiContentId("123").orElseThrow().getId());
        assertEquals(mappingId, modes.findBySpot_Id(id).getFirst().getId());
    }

    @Test void reimportReplacesModesAndClearsOptionalValues() {
        Long id = imports.receive("test-key", request("First", List.of(Mode.NATURAL, Mode.SENSORY), source, "https://example.com/a.jpg"))
                .spots().getFirst().spotId();
        clear();
        imports.receive("test-key", request("Updated", List.of(Mode.SENSORY, Mode.VINTAGE), source.plusHours(1), null));
        clear();
        var detail = reads.findById(null, id);
        assertEquals("Updated", detail.name());
        assertNull(detail.imageUrl());
        assertEquals(2, detail.modes().size());
        assertFalse(detail.modes().contains("NATURAL"));
        assertTrue(detail.modes().contains("VINTAGE"));
        imports.receive("test-key", request("Updated", List.of(), source.plusHours(2), null));
        clear();
        assertTrue(modes.findBySpot_Id(id).isEmpty());
    }

    @Test void staleSnapshotDoesNotOverwriteBasicInfoOrModes() {
        imports.receive("test-key", request("Latest", List.of(Mode.NATURAL), source.plusHours(1), null));
        clear();
        var result = imports.receive("test-key", request("Old", List.of(Mode.VINTAGE), source, null));
        clear();
        assertEquals(1, result.ignoredStale());
        var spot = spots.findByTourApiContentId("123").orElseThrow();
        assertEquals("Latest", spot.getName());
        assertEquals(List.of(Mode.NATURAL), modes.findBySpot_Id(spot.getId()).stream().map(SpotMode::getMode).toList());
    }

    @Test void basicImportConnectsToQuietPushAndPreservesScoreOnReimport() {
        Long id = imports.receive("test-key", request("First", List.of(Mode.NATURAL), source, null)).spots().getFirst().spotId();
        clear();
        var observedAt = source.plusMinutes(30).toLocalDateTime();
        quiet.push("test-key", new QuietIndexPushRequest("123", 83, observedAt, null));
        clear();
        imports.receive("test-key", request("Updated", List.of(Mode.SENSORY), source.plusHours(1), null));
        clear();
        assertEquals(83, reads.findById(null, id).quietScore());
        assertEquals(observedAt, spots.findById(id).orElseThrow().getQuietScoreUpdatedAt());
        assertEquals(1, quietIndices.count());
    }

    @Test void existingVisitLikeAndReviewReferencesSurviveReimport() {
        Long id = imports.receive("test-key", request("First", List.of(), source, null)).spots().getFirst().spotId();
        clear();
        var spot = spots.findById(id).orElseThrow();
        var user = User.builder().googleSub("test-user").email("test@example.com").build();
        em.persist(user);
        var visit = Visit.builder().user(user).spot(spot).build();
        visit.complete();
        em.persist(visit);
        var like = SpotLike.builder().user(user).spot(spot).build();
        em.persist(like);
        var review = Review.builder().visit(visit).rating(5).content("Good").build();
        em.persist(review);
        Long visitId = visit.getId();
        Long likeId = like.getId();
        Long reviewId = review.getId();
        clear();
        imports.receive("test-key", request("Updated", List.of(Mode.NATURAL), source.plusHours(1), null));
        clear();
        assertEquals(id, em.find(Visit.class, visitId).getSpot().getId());
        assertEquals(id, em.find(SpotLike.class, likeId).getSpot().getId());
        assertEquals(id, em.find(Review.class, reviewId).getSpot().getId());
    }

    private SpotImportRequest request(String name, List<Mode> modes, OffsetDateTime updated, String image) {
        return new SpotImportRequest(List.of(new SpotImportRequest.Item("123", name, "Busan", BigDecimal.valueOf(35),
                BigDecimal.valueOf(129), Category.PARK, null, image, null, modes, updated)));
    }
    private void clear() { em.flush(); em.clear(); }
}
