package com.coltrip.backend.review.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.domain.like.SpotLikeRepository;
import com.coltrip.backend.domain.review.ReviewRepository;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.like.controller.SpotLikeController;
import com.coltrip.backend.like.service.SpotLikeService;
import com.coltrip.backend.review.controller.ReviewController;
import com.coltrip.backend.visit.dto.VisitCompleteRequest;
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.service.VisitService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class LikeReviewConcurrencyIntegrationTest {
    @Autowired private SpotLikeService likeService;
    @Autowired private ReviewService reviewService;
    @Autowired private VisitService visitService;
    @Autowired private UserRepository users;
    @Autowired private TouristSpotRepository spots;
    @Autowired private SpotLikeRepository likes;
    @Autowired private ReviewRepository reviews;
    @Autowired private VisitRepository visits;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @Value("${test.require-mysql:false}") private boolean requireMysql;

    private MockMvc mvc;
    private Long userId;
    private Long otherUserId;
    private Long spotId;
    private Long visitId;
    private static final BigDecimal LAT = new BigDecimal("35.1");
    private static final BigDecimal LNG = new BigDecimal("129.1");

    @BeforeEach
    void setUp() throws Exception {
        if (requireMysql) {
            try (var connection = dataSource.getConnection()) {
                assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            }
        }
        mvc = MockMvcBuilders.standaloneSetup(new SpotLikeController(likeService),
                        new ReviewController(reviewService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        String suffix = UUID.randomUUID().toString();
        userId = saveUser("owner-" + suffix);
        otherUserId = saveUser("other-" + suffix);
        spotId = spots.save(TouristSpot.builder().tourApiContentId("RACE-" + suffix)
                .name("Concurrency test").address("Busan").latitude(LAT).longitude(LNG)
                .category(Category.CAFE).build()).getId();
        visitId = visitService.start(userId, new VisitStartRequest(spotId, LAT, LNG)).visitId();
        visitService.complete(userId, visitId, new VisitCompleteRequest(LAT, LNG));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (userId != null) {
                reviews.deleteByUser_Id(userId);
                likes.deleteByUser_Id(userId);
                visits.deleteByUser_Id(userId);
            }
            if (spotId != null) spots.deleteById(spotId);
            if (userId != null) users.deleteById(userId);
            if (otherUserId != null) users.deleteById(otherUserId);
        });
    }

    @Test
    void concurrentLikesAllReturn200AndStoreOneRow() throws Exception {
        var results = concurrent(() -> request(userId, post("/api/spots/{id}/like", spotId)));
        for (var result : results) {
            status().isOk().match(result);
            jsonPath("$.liked").value(true).match(result);
        }
        assertEquals(1, likes.countByUser_Id(userId));
    }

    @Test
    void concurrentUnlikesAllReturn200AndLeaveNoRows() throws Exception {
        likeService.like(userId, spotId);
        var results = concurrent(() -> request(userId, delete("/api/spots/{id}/like", spotId)));
        for (var result : results) {
            status().isOk().match(result);
            jsonPath("$.liked").value(false).match(result);
        }
        assertEquals(0, likes.countByUser_Id(userId));
    }

    @Test
    void concurrentReviewsReturnOne200AndRemaining409() throws Exception {
        var results = concurrent(() -> request(userId, reviewRequest()));
        int success = 0;
        for (var result : results) {
            if (result.getResponse().getStatus() == 200) {
                success++;
            } else {
                status().isConflict().match(result);
                jsonPath("$.code").value("ReviewNotAllowedException").match(result);
            }
        }
        assertEquals(1, success);
        assertEquals(1, reviews.findByVisit_IdIn(List.of(visitId)).size());
    }

    @Test
    void nonOwnerStillGets404() throws Exception {
        status().isNotFound().match(request(otherUserId, reviewRequest()));
        assertFalse(reviews.existsByVisit_Id(visitId));
    }

    @Test
    void incompleteVisitStillGets409() throws Exception {
        Long activeId = visitService.start(userId, new VisitStartRequest(spotId, LAT, LNG)).visitId();
        var result = request(userId, post("/api/visits/{id}/review", activeId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"));
        status().isConflict().match(result);
        assertFalse(reviews.existsByVisit_Id(activeId));
    }

    @Test
    void missingSpotStillGets404() throws Exception {
        status().isNotFound().match(request(userId, post("/api/spots/{id}/like", Long.MAX_VALUE)));
        assertEquals(0, likes.countByUser_Id(userId));
    }

    private Long saveUser(String sub) {
        return users.save(User.builder().googleSub(sub).email(sub + "@example.test").build()).getId();
    }

    private MockHttpServletRequestBuilder reviewRequest() {
        return post("/api/visits/{id}/review", visitId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"content\":\"Concurrent review\"}");
    }

    private MvcResult request(Long principal, MockHttpServletRequestBuilder builder) throws Exception {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SecurityContextHolder.setContext(context);
        try {
            return mvc.perform(builder).andReturn();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<MvcResult> concurrent(Callable<MvcResult> call) throws Exception {
        int count = 4;
        try (var pool = Executors.newFixedThreadPool(count)) {
            var ready = new CountDownLatch(count);
            var go = new CountDownLatch(1);
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!go.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    return call.call();
                }));
            }
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                go.countDown();
            }
            List<MvcResult> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        }
    }
}
