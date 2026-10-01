package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.posts.PostInternalAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@PostgresTestcontainers
@Import(PostIdempotencyManagement.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PostIdempotencyManagementConcurrencyIT {

    @MockitoBean
    PostInternalAPI postAPI;

    @Autowired
    PostCreationRequestRepository repo;

    @Autowired
    PostIdempotencyManagement service;

    final UUID authorId = UUID.randomUUID();
    final UUID postId = UUID.randomUUID();
    final String idempotencyKey = "idempotencyKeyidempotencyKey";
    final String requestHash = "requestHashrequestHashrequestHashrequestHash";

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
    void whenSecondRequestArrivesWhileFirstInProgress_thenConflict() throws Exception {
        var firstInsideCallback = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);

        Future<UUID> first = pool.submit(() -> service.executeIdempotent(
                authorId, idempotencyKey, requestHash, () -> {
                    firstInsideCallback.countDown();
                    await(releaseFirst, 5, TimeUnit.SECONDS);
                    return postId;
                }));

        assertTrue(firstInsideCallback.await(5, TimeUnit.SECONDS),
                "first thread never reached the callback");

        Future<ResponseStatusException> second = pool.submit(() ->
                assertThrows(ResponseStatusException.class, () ->
                        service.executeIdempotent(
                                authorId, idempotencyKey, requestHash, () -> postId
                        )));

        try {
            ResponseStatusException ex = second.get(5, TimeUnit.SECONDS);
            assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());

            releaseFirst.countDown();
            assertEquals(postId, first.get(5, TimeUnit.SECONDS));

            assertEquals(1, repo.findAll().size());
        } finally {
            releaseFirst.countDown();
        }
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
