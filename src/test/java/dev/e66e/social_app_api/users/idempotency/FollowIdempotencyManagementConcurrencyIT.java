package dev.e66e.social_app_api.users.idempotency;

import dev.e66e.social_app_api.PostgresTestcontainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@PostgresTestcontainers
@Import(FollowRelationshipIdempotencyManagement.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("FollowRelationshipIdempotencyManagement Concurrency Integration Tests")
class FollowIdempotencyManagementConcurrencyIT {

    @Autowired
    FollowRelationshipRequestRepository repo;

    @Autowired
    FollowRelationshipIdempotencyManagement service;

    final UUID followerId = UUID.randomUUID();
    final UUID followTargetId = UUID.randomUUID();
    final String idempotencyKey = "idempotencyKeyidempotencyKeyidemidempotencyKeyidempotencyKey";
    final String requestHash = "requestHashrequestHashrequestHasrequestHashrequestHashrequestHas";

    ExecutorService pool;

    @BeforeEach
    void setUp() {
        pool = Executors.newFixedThreadPool(2);
        repo.deleteAll();
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
        repo.deleteAll();
    }

    @Test
    @DisplayName("when second request arrives while first is in progress, second receives 409 Conflict")
    void whenSecondRequestArrivesWhileFirstInProgress_thenConflict() throws Exception {
        var firstInsideCallback = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);

        Future<UUID> first = pool.submit(() -> service.executeIdempotent(
                followerId, idempotencyKey, requestHash, () -> {
                    firstInsideCallback.countDown();
                    await(releaseFirst, 5, TimeUnit.SECONDS);
                    return followTargetId;
                }));

        assertTrue(firstInsideCallback.await(5, TimeUnit.SECONDS),
                "first thread never reached the callback");

        Future<ResponseStatusException> second = pool.submit(() ->
                assertThrows(ResponseStatusException.class, () ->
                        service.executeIdempotent(
                                followerId, idempotencyKey, requestHash, () -> followTargetId
                        )));

        try {
            ResponseStatusException ex = second.get(5, TimeUnit.SECONDS);
            assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());

            releaseFirst.countDown();
            assertEquals(followTargetId, first.get(5, TimeUnit.SECONDS));

            assertEquals(1, repo.findAll().size());
        } finally {
            releaseFirst.countDown();
        }
    }

    @Test
    @DisplayName("when second request arrives after first completes, second receives cached result without executing callback")
    void whenSecondRequestArrivesAfterFirstCompletes_thenReturnsSameResultWithoutExecutingCallback() throws Exception {
        AtomicInteger callbackExecutionCount = new AtomicInteger(0);

        // First request executes and completes
        UUID firstResult = service.executeIdempotent(
                followerId, idempotencyKey, requestHash, () -> {
                    callbackExecutionCount.incrementAndGet();
                    return followTargetId;
                });
        assertEquals(followTargetId, firstResult);
        assertEquals(1, callbackExecutionCount.get());

        // Second request with same key and hash should return cached result
        Future<UUID> second = pool.submit(() ->
                service.executeIdempotent(followerId, idempotencyKey, requestHash, () -> {
                    callbackExecutionCount.incrementAndGet();
                    return followTargetId;
                }));

        UUID secondResult = second.get(5, TimeUnit.SECONDS);
        assertEquals(followTargetId, secondResult);
        assertEquals(1, callbackExecutionCount.get(), "Callback must not be executed a second time");
        assertEquals(1, repo.findAll().size());
    }

    private static void await(CountDownLatch latch, long t, TimeUnit unit) {
        try {
            if (!latch.await(t, unit))
                throw new IllegalStateException("latch timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
