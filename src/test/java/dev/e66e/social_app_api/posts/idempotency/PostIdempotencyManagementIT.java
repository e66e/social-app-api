package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles({"test"})
@PostgresTestcontainers
@Import(PostIdempotencyManagement.class)
class PostIdempotencyManagementIT {

    @Autowired
    PostCreationRequestRepository repo;

    @Autowired
    PostIdempotencyManagement postIdempotencyManagement;

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class ExecuteIdempotent {

        final UUID authorId = UUID.randomUUID();
        final UUID postId = UUID.randomUUID();
        final String requestHash = "requestHashrequestHashrequestHas";
        final String idempotencyKey = "idempotencyKeyidempotencyKeyidemptencyKey";


        final AtomicInteger counter = new AtomicInteger(0);
        final Supplier<UUID> cb = () -> {
            counter.incrementAndGet();
            return postId;
        };

        @AfterEach
        void cleanup() {
            repo.deleteAll();
            counter.set(0);
        }

        @Test
        void whenRaceWon_createPostRequestThenReturnPost() {
            // Given

            // When
            UUID out = postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            PostCreationRequest idempotencyRecord = repo.findByIdempotencyKey(authorId, idempotencyKey)
                    .orElseThrow(() -> new RuntimeException("Post Idempotent failed"));

            assertEquals(IdempotencyStatus.COMPLETED, idempotencyRecord.getStatus());
            assertEquals(postId, idempotencyRecord.getResourceId());
            assertEquals(authorId, idempotencyRecord.getAuthorId());
            assertEquals(idempotencyKey, idempotencyRecord.getIdempotencyKey());
            assertEquals(requestHash, idempotencyRecord.getRequestHash());
            assertNotNull(idempotencyRecord.getCreatedAt());
            assertNotNull(idempotencyRecord.getUpdatedAt());
            assertNotNull(out);
            assertEquals(1, counter.get());
        }

        @Test
        void whenRaceWon_andCallbackThrows_thenRemoveIdempotencyRecordAndRethrows() {
            // Given
            Supplier<UUID> cb = () -> {
                throw new RuntimeException("Simulating database error");
            };

            // When / Then
            assertThrows(RuntimeException.class, () -> postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb),
                    "Simulating database error");

            assertTrue(repo.findByIdempotencyKey(authorId, idempotencyKey).isEmpty());
        }

        @Test
        void whenRaceLost_thenReturnCompletedPost() {
            // Given
            PostCreationRequest entity = new PostCreationRequest(authorId, idempotencyKey, requestHash);
            entity.setResourceId(postId);
            entity.setStatus(IdempotencyStatus.COMPLETED);
            repo.saveAndFlush(entity);

            // When
            UUID out = postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(postId, out);

            assertEquals(0, counter.get());
        }

        @Test
        void whenRaceLost_AndThereWasRecordInProgress_thenThrowConflict() {
            // Given
            repo.saveAndFlush(new PostCreationRequest(authorId, idempotencyKey, requestHash));

            // When / Then
            ResponseStatusException exc = assertThrows(ResponseStatusException.class,
                    () -> postIdempotencyManagement
                        .executeIdempotent(authorId, idempotencyKey, requestHash, cb));

            assertEquals(HttpStatus.CONFLICT.value(), exc.getStatusCode().value());
        }
    }
}
