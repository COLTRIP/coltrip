package com.coltrip.backend.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.domain.user.User;
import com.coltrip.backend.domain.user.UserRepository;
import com.coltrip.backend.domain.visit.VisitRepository;
import com.coltrip.backend.domain.visit.VisitStatus;
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.exception.AlreadyOngoingVisitException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 로컬/CI의 실제 MySQL을 대상으로 동시 요청 경쟁 조건을 재현하는 통합 테스트.
// UserRepository.findByIdForUpdate의 행 잠금이 없으면 두 스레드 모두 성공해 STARTED 방문이 2건 생성된다.
@SpringBootTest
class VisitServiceConcurrencyTest {

    @Autowired
    private VisitService visitService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TouristSpotRepository touristSpotRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long userId;
    private Long spotAId;
    private Long spotBId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .googleSub("concurrency-test-" + suffix)
                .email("concurrency-test-" + suffix + "@coltrip.dev")
                .build());
        userId = user.getId();

        spotAId = saveSpot("CONCURRENCY-TEST-A-" + suffix).getId();
        spotBId = saveSpot("CONCURRENCY-TEST-B-" + suffix).getId();
    }

    @AfterEach
    void tearDown() {
        // deleteByUser_Id는 파생 삭제 쿼리라 호출 측에서 트랜잭션을 열어줘야 동작한다.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            visitRepository.deleteByUser_Id(userId);
            touristSpotRepository.deleteById(spotAId);
            touristSpotRepository.deleteById(spotBId);
            userRepository.deleteById(userId);
        });
    }

    @Test
    void concurrentStartRequestsCreateOnlyOneStartedVisit() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Long> spotIds = List.of(spotAId, spotBId);
        for (Long spotId : spotIds) {
            pool.submit(() -> attemptStart(spotId, ready, go, successCount, conflictCount));
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successCount.get());
        assertEquals(1, conflictCount.get());
        assertEquals(1, visitRepository.countByUser_IdAndStatus(userId, VisitStatus.STARTED));
    }

    private void attemptStart(Long spotId, CountDownLatch ready, CountDownLatch go,
                               AtomicInteger successCount, AtomicInteger conflictCount) {
        try {
            ready.countDown();
            go.await();
            visitService.start(userId, new VisitStartRequest(spotId, BigDecimal.valueOf(35.0), BigDecimal.valueOf(129.0)));
            successCount.incrementAndGet();
        } catch (AlreadyOngoingVisitException e) {
            conflictCount.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private TouristSpot saveSpot(String tourApiContentId) {
        return touristSpotRepository.save(TouristSpot.builder()
                .tourApiContentId(tourApiContentId)
                .name("동시성 테스트 장소")
                .address("부산 테스트 주소")
                .latitude(BigDecimal.valueOf(35.0))
                .longitude(BigDecimal.valueOf(129.0))
                .category(Category.CAFE)
                .build());
    }
}
