package com.coltrip.backend.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coltrip.backend.auth.google.GoogleTokenVerifier;
import com.coltrip.backend.auth.google.GoogleUserInfo;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.Visit;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.config.JwtProperties;
import com.coltrip.backend.quietindex.service.QuietIndexMapSyncScheduler;
import com.coltrip.backend.visit.nudge.BackgroundNudgeScheduler;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class DemoModeIntegrationTest {
    private static final String KEY = "demo-only-integration-secret-12345678901234567890";
    private static final String URL = "jdbc:h2:mem:coltrip_demo;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1";
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("demo.enabled", () -> "true");
        r.add("app.environment", () -> "demo");
        r.add("demo.allowed-google-subs", () -> "allowed,allowed-other");
        r.add("demo.datasource-url", () -> URL); r.add("spring.datasource.url", () -> URL);
        r.add("demo.datasource-username", () -> "sa"); r.add("spring.datasource.username", () -> "sa");
        r.add("demo.datasource-password", () -> "demo-test"); r.add("spring.datasource.password", () -> "demo-test");
        r.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("demo.jwt-secret", () -> KEY); r.add("jwt.secret", () -> KEY);
        r.add("fcm.credentials-path", () -> "");
    }
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired JwtProvider jwt;
    @Autowired UserRepository users;
    @Autowired VisitRepository visits;
    @MockitoBean GoogleTokenVerifier google;
    MockMvc mvc;
    User allowed;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        allowed = user("allowed");
    }

    @Test void anonymousAndNonAllowedUsersCannotReadPublicDataOrUseDemoFlags() throws Exception {
        mvc.perform(get("/api/spots/recommendations/current").param("demo", "true").header("X-Demo", "true"))
                .andExpect(status().isUnauthorized());
        User denied = user("denied");
        mvc.perform(get("/api/users/me").header("Authorization", bearer(denied)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DemoAccessDenied"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt.createAccessToken(999999L)))
                .andExpect(status().isForbidden());
    }

    @Test void allowedUserCanQueryBusanButInternalAndPushRemainDisabled() throws Exception {
        var spot = TouristSpot.builder().tourApiContentId(UUID.randomUUID().toString()).name("부산 시연 장소")
                .address("부산").latitude(new BigDecimal("35.1796")).longitude(new BigDecimal("129.0756"))
                .category(Category.PARK).build();
        em.persist(spot);
        mvc.perform(get("/api/spots/recommendations/current").header("Authorization", bearer(allowed)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("CURRENT"))
                .andExpect(jsonPath("$.spots[0].spot.name").value("부산 시연 장소"));
        mvc.perform(post("/api/internal/spots").header("Authorization", bearer(allowed))
                        .header("X-Internal-Api-Key", "irrelevant").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertTrue(context.getBeansOfType(FirebaseMessaging.class).isEmpty());
        assertTrue(context.getBeansOfType(BackgroundNudgeScheduler.class).isEmpty());
        assertTrue(context.getBeansOfType(QuietIndexMapSyncScheduler.class).isEmpty());
    }

    @Test void googleVerificationOccursBeforeAllowlistAndDeniedSignupDoesNotWrite() throws Exception {
        when(google.verify("allowed-token")).thenReturn(new GoogleUserInfo("allowed", allowed.getEmail()));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"allowed-token\",\"intent\":\"LOGIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        when(google.verify("denied-token")).thenReturn(new GoogleUserInfo("new-denied", "denied@example.test"));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"denied-token\",\"intent\":\"SIGNUP\"}"))
                .andExpect(status().isForbidden());
        assertFalse(users.existsByGoogleSub("new-denied"));
        verify(google).verify("denied-token");
    }

    @Test void refreshDenyDoesNotRotateAndProductionKeyCannotAuthenticate() throws Exception {
        User denied = user("denied");
        String old = jwt.createRefreshToken(denied.getId());
        denied.updateRefreshToken(old);
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + old))
                .andExpect(status().isForbidden());
        assertEquals(old, denied.getRefreshToken());
        var otherEnvironmentJwt = new JwtProvider(new JwtProperties("other-environment-test-key-12345678901234567890", 3600, 1209600));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + otherEnvironmentJwt.createAccessToken(allowed.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test void allowedRefreshRotatesButInvalidTokenStillReturns401() throws Exception {
        String old = jwt.createRefreshToken(allowed.getId());
        allowed.updateRefreshToken(old);
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + old))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").isNotEmpty());
        assertNotEquals(old, allowed.getRefreshToken());
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + old))
                .andExpect(status().isUnauthorized());
    }

    @Test void nonAllowedUsersCannotWriteVisits() throws Exception {
        User denied = user("denied");
        long count = visits.count();
        mvc.perform(post("/api/visits/start").header("Authorization", bearer(denied))
                        .param("demo", "true").contentType(MediaType.APPLICATION_JSON).content("{\"spotId\":1}"))
                .andExpect(status().isForbidden());
        assertEquals(count, visits.count());
    }

    @Test void visitOwnershipAndStateAreNotBypassed() throws Exception {
        var spot = TouristSpot.builder().tourApiContentId(UUID.randomUUID().toString()).name("부산 시연 장소")
                .address("부산").latitude(new BigDecimal("35.1796")).longitude(new BigDecimal("129.0756"))
                .category(Category.PARK).build();
        em.persist(spot);
        var body = mvc.perform(post("/api/visits/start").header("Authorization", bearer(allowed))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spotId\":" + spot.getId() + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long visitId = new ObjectMapper().readTree(body).get("visitId").asLong();
        mvc.perform(post("/api/visits/start").header("Authorization", bearer(allowed))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spotId\":" + spot.getId() + "}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/visits/{id}/complete", visitId).header("Authorization", bearer(allowed)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(patch("/api/visits/{id}/complete", visitId).header("Authorization", bearer(allowed)))
                .andExpect(status().isConflict());
        var other = user("allowed-other");
        var otherVisit = visits.save(Visit.builder().user(other).spot(spot).build());
        mvc.perform(patch("/api/visits/{id}/cancel", otherVisit.getId()).header("Authorization", bearer(allowed)))
                .andExpect(status().isNotFound());
        assertEquals(VisitStatus.STARTED, otherVisit.getStatus());
    }

    private User user(String sub) {
        User user = User.builder().googleSub(sub).email(UUID.randomUUID() + "@example.test").build();
        em.persist(user);
        return user;
    }
    private String bearer(User user) { return "Bearer " + jwt.createAccessToken(user.getId()); }
}
