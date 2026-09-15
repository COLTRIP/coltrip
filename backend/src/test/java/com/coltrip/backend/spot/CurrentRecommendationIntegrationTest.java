package com.coltrip.backend.spot;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.like.SpotLike;
import com.coltrip.backend.domain.spot.*;
import com.coltrip.backend.domain.user.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class CurrentRecommendationIntegrationTest {
    private static final String URL = "/api/spots/recommendations/current";
    private static final LocalDateTime OBSERVED = LocalDateTime.of(2026, 9, 14, 22, 5);
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired JwtProvider jwt;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void currentScoreMatchesMapAndDetailWithoutForecastData() throws Exception {
        var spot = spot("10", 98, Category.BEACH);
        flush();
        mvc.perform(request()).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.type").value("CURRENT"))
                .andExpect(jsonPath("$.spots[0].spot.id").value(spot.getId()))
                .andExpect(jsonPath("$.spots[0].spot.quietScore").value(98))
                .andExpect(jsonPath("$.spots[0].spot.quietLevel").value("QUIET"))
                .andExpect(jsonPath("$.spots[0].spot.quietScoreUpdatedAt").value("2026-09-14T22:05:00"))
                .andExpect(jsonPath("$.spots[0].forecast").doesNotExist());
        mvc.perform(get("/api/spots/{id}", spot.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.quietScore").value(98))
                .andExpect(jsonPath("$.quietScoreUpdatedAt").value("2026-09-14T22:05:00"));
        mvc.perform(get("/api/spots").param("swLat", "9.9").param("neLat", "10.1")
                        .param("swLng", "19.9").param("neLng", "20.1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots[0].quietScore").value(98));
    }

    @Test void sortsScoreDescThenIdAscAndNullLastBeforeLimiting() throws Exception {
        // 로컬 개발 DB의 기존 시드 장소(12곳)는 spot_mode가 없으므로 mode 필터로 격리한다.
        var unknown = taggedSpot("10", null, Category.BEACH);
        var zero = taggedSpot("10", 0, Category.BEACH);
        var first = taggedSpot("10", 90, Category.BEACH);
        var second = taggedSpot("10", 90, Category.BEACH);
        var highest = taggedSpot("11", 100, Category.BEACH);
        flush();
        mvc.perform(request().param("mode", "TRANQUIL")).andExpect(status().isOk())
                .andExpect(jsonPath("$.spots[*].spot.id", contains(highest.getId().intValue(), first.getId().intValue(),
                        second.getId().intValue(), zero.getId().intValue(), unknown.getId().intValue())))
                .andExpect(jsonPath("$.spots[4].spot.quietScore").isEmpty())
                .andExpect(jsonPath("$.spots[4].spot.quietLevel").isEmpty())
                .andExpect(jsonPath("$.spots[4].spot.quietScoreUpdatedAt").isEmpty());
        mvc.perform(request().param("mode", "TRANQUIL").param("limit", "2")).andExpect(jsonPath("$.spots", hasSize(2)))
                .andExpect(jsonPath("$.spots[0].spot.id").value(highest.getId()));
    }

    @Test void filtersKeepAllModesAndPersonalizeOnlyOwnersLikes() throws Exception {
        var spot = spot("10", null, Category.PARK);
        em.persist(SpotMode.builder().spot(spot).mode(Mode.NATURAL).build());
        em.persist(SpotMode.builder().spot(spot).mode(Mode.TRANQUIL).build());
        spot("10", 100, Category.BEACH);
        var user = User.builder().googleSub(UUID.randomUUID().toString()).email(UUID.randomUUID() + "@example.test").build();
        em.persist(user);
        em.persist(SpotLike.builder().user(user).spot(spot).build());
        flush();
        mvc.perform(request().param("category", "PARK").param("mode", "NATURAL")
                        .header("Authorization", "Bearer " + jwt.createAccessToken(user.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spots", hasSize(1)))
                .andExpect(jsonPath("$.spots[0].spot.modes", containsInAnyOrder("NATURAL", "TRANQUIL")))
                .andExpect(jsonPath("$.spots[0].spot.isLiked").value(true));
        for (String token : new String[]{"invalid", jwt.createAccessToken(user.getId() + 100000)}) {
            mvc.perform(request().param("category", "PARK").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.spots[0].spot.isLiked").value(false));
        }
    }

    @Test void emptyAndAllMissingScoresRemainSuccessful() throws Exception {
        // 로컬 개발 DB의 기존 시드 장소(12곳)는 spot_mode가 없으므로 mode 필터로 격리한다.
        mvc.perform(request().param("mode", "TRANQUIL")).andExpect(status().isOk())
                .andExpect(jsonPath("$.spots", hasSize(0))).andExpect(jsonPath("$.message").isNotEmpty());
        taggedSpot("10", null, Category.BEACH);
        flush();
        mvc.perform(request().param("mode", "TRANQUIL")).andExpect(status().isOk())
                .andExpect(jsonPath("$.spots", hasSize(1)))
                .andExpect(jsonPath("$.spots[0].spot.imageUrl").isEmpty())
                .andExpect(jsonPath("$.spots[0].spot.quietScore").isEmpty());
    }

    @Test void noLocationRequiredAndLimitValidation() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk());
        for (String[] input : new String[][]{{"limit", "0"}, {"limit", "51"}, {"limit", "abc"},
                {"category", "invalid"}, {"mode", "invalid"}}) {
            mvc.perform(request().param(input[0], input[1])).andExpect(status().isBadRequest());
        }
        mvc.perform(request().param("limit", "50")).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder request() {
        return get(URL);
    }

    private TouristSpot spot(String latitude, Integer score, Category category) {
        var spot = TouristSpot.builder().tourApiContentId(UUID.randomUUID().toString()).name("테스트 장소")
                .address("테스트 주소").latitude(new BigDecimal(latitude)).longitude(new BigDecimal("20"))
                .category(category).build();
        if (score != null) spot.updateQuietScoreIfNewer(score, OBSERVED);
        em.persist(spot);
        return spot;
    }

    private TouristSpot taggedSpot(String latitude, Integer score, Category category) {
        var spot = spot(latitude, score, category);
        em.persist(SpotMode.builder().spot(spot).mode(Mode.TRANQUIL).build());
        return spot;
    }

    private void flush() { em.flush(); em.clear(); }
}
