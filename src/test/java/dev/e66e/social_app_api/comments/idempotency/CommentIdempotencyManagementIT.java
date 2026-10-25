package dev.e66e.social_app_api.comments.idempotency;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
@Import(CommentIdempotencyManagement.class)
class CommentIdempotencyManagementIT {

    @Autowired
    CommentCreationRequestRepository repo;

    @Autowired
    CommentIdempotencyManagement commentIdempotencyManagement;

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class ExecuteIdempotent {

        final UUID authorId = UUID.randomUUID();
        final UUID commentId = UUID.randomUUID();
        final String requestHash = "a".repeat(64);
        final String idempotencyKey = "idempotencyKeyidempotencyKeyidemptencyKey";


        final AtomicInteger counter = new AtomicInteger(0);
        final Supplier<UUID> cb = () -> {
            counter.incrementAndGet();
            return commentId;
        };

        @AfterEach
        void cleanup() {
            repo.deleteAll();
            counter.set(0);
        }

        @Test
        void whenRaceWon_createCommentRequestThenReturnComment() {
            // Given

            // When
            UUID out = commentIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            CommentCreationRequest idempotencyRecord = repo.findByIdempotencyKey(authorId, idempotencyKey)
                    .orElseThrow(() -> new RuntimeException("Comment Idempotent failed"));

            assertEquals(IdempotencyStatus.COMPLETED, idempotencyRecord.getStatus());
            assertEquals(commentId, idempotencyRecord.getResourceId());
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
            assertThrows(RuntimeException.class, () -> commentIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb),
                    "Simulating database error");

            assertTrue(repo.findByIdempotencyKey(authorId, idempotencyKey).isEmpty());
        }

        @Test
        void whenRaceLost_thenReturnCompletedComment() {
            // Given
            CommentCreationRequest entity = new CommentCreationRequest(authorId, idempotencyKey, requestHash);
            entity.setResourceId(commentId);
            entity.setStatus(IdempotencyStatus.COMPLETED);
            repo.saveAndFlush(entity);

            // When
            UUID out = commentIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(commentId, out);

            assertEquals(0, counter.get());
        }

        @Test
        void whenRaceLost_AndThereWasRecordInProgress_thenThrowConflict() {
            // Given
            repo.saveAndFlush(new CommentCreationRequest(authorId, idempotencyKey, requestHash));

            // When / Then
            ResponseStatusException exc = assertThrows(ResponseStatusException.class,
                    () -> commentIdempotencyManagement
                        .executeIdempotent(authorId, idempotencyKey, requestHash, cb));

            assertEquals(HttpStatus.CONFLICT.value(), exc.getStatusCode().value());
        }
    }

    @Nested
    class CommentIdempotencyRecordEntity {

        @Test
        void whenIdempotencyKeyTooShort_throws() {
            UUID authorId = UUID.randomUUID();
            String requestHash = "a".repeat(64);
            CommentCreationRequest entity = new CommentCreationRequest(authorId, "b", requestHash);

            assertThrows(DataIntegrityViolationException.class, () -> repo.saveAndFlush(entity));
        }
    }
}
