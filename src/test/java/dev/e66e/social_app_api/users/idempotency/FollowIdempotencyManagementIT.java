package dev.e66e.social_app_api.users.idempotency;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
@Import(FollowRelationshipIdempotencyManagement.class)
@DisplayName("FollowRelationshipIdempotencyManagement Integration Tests")
class FollowIdempotencyManagementIT {

    @Autowired
    FollowRelationshipRequestRepository repo;

    @Autowired
    FollowRelationshipIdempotencyManagement followRelationshipIdempotencyManagement;

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("executeIdempotent")
    class ExecuteIdempotent {

        final UUID followerId = UUID.randomUUID();
        final UUID followTargetId = UUID.randomUUID();
        final String requestHash = "requestHashrequestHashrequestHasrequestHashrequestHashrequestHas";
        final String idempotencyKey = "idempotencyKeyidempotencyKeyidemptencyKey";

        final AtomicInteger counter = new AtomicInteger(0);
        final Supplier<UUID> cb = () -> {
            counter.incrementAndGet();
            return followTargetId;
        };

        @AfterEach
        void cleanup() {
            repo.deleteAll();
            counter.set(0);
        }

        @Test
        @DisplayName("when race is won, executes callback, persists completed record and returns target ID")
        void whenRaceWon_createFollowRequestThenReturnFollowTarget() {
            // When
            UUID out = followRelationshipIdempotencyManagement
                    .executeIdempotent(followerId, idempotencyKey, requestHash, cb);

            // Then
            FollowRelationshipRequest idempotencyRecord = repo.findByIdempotencyKey(followerId, idempotencyKey)
                    .orElseThrow(() -> new RuntimeException("Follow relationship idempotency record not found"));

            assertEquals(IdempotencyStatus.COMPLETED, idempotencyRecord.getStatus());
            assertEquals(followTargetId, idempotencyRecord.getResourceId());
            assertEquals(followerId, idempotencyRecord.getAuthorId());
            assertEquals(idempotencyKey, idempotencyRecord.getIdempotencyKey());
            assertEquals(requestHash, idempotencyRecord.getRequestHash());
            assertNotNull(idempotencyRecord.getCreatedAt());
            assertNotNull(idempotencyRecord.getUpdatedAt());
            assertNotNull(out);
            assertEquals(1, counter.get());
        }

        @Test
        @DisplayName("when race is won but callback throws, removes idempotency record and rethrows exception")
        void whenRaceWon_andCallbackThrows_thenRemoveIdempotencyRecordAndRethrows() {
            // Given
            Supplier<UUID> failingCb = () -> {
                throw new RuntimeException("Simulating database error");
            };

            // When / Then
            assertThrows(RuntimeException.class, () -> followRelationshipIdempotencyManagement
                            .executeIdempotent(followerId, idempotencyKey, requestHash, failingCb),
                    "Simulating database error");

            assertTrue(repo.findByIdempotencyKey(followerId, idempotencyKey).isEmpty());
        }

        @Test
        @DisplayName("when race is lost and record is completed with matching hash, returns cached target ID without running callback")
        void whenRaceLost_thenReturnCompletedFollowTarget() {
            // Given
            FollowRelationshipRequest entity = new FollowRelationshipRequest(followerId, idempotencyKey, requestHash);
            entity.setResourceId(followTargetId);
            entity.setStatus(IdempotencyStatus.COMPLETED);
            repo.saveAndFlush(entity);

            // When
            UUID out = followRelationshipIdempotencyManagement
                    .executeIdempotent(followerId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(followTargetId, out);
            assertEquals(0, counter.get());
        }

        @Test
        @DisplayName("when race is lost and record is currently in progress, throws 409 Conflict")
        void whenRaceLost_AndThereWasRecordInProgress_thenThrowConflict() {
            // Given
            repo.saveAndFlush(new FollowRelationshipRequest(followerId, idempotencyKey, requestHash));

            // When / Then
            ResponseStatusException exc = assertThrows(ResponseStatusException.class,
                    () -> followRelationshipIdempotencyManagement
                            .executeIdempotent(followerId, idempotencyKey, requestHash, cb));

            assertEquals(HttpStatus.CONFLICT.value(), exc.getStatusCode().value());
        }

        @Test
        @DisplayName("when race is lost, record is completed but request hash differs, throws 409 Conflict")
        void whenRaceLost_andHashDiffers_thenThrowConflict() {
            // Given
            FollowRelationshipRequest entity = new FollowRelationshipRequest(followerId, idempotencyKey, requestHash);
            entity.setResourceId(followTargetId);
            entity.setStatus(IdempotencyStatus.COMPLETED);
            repo.saveAndFlush(entity);

            String differentRequestHash = "differentRequestHashdifferentRequestHashdifferentRequestHash1234";

            // When / Then
            ResponseStatusException exc = assertThrows(ResponseStatusException.class,
                    () -> followRelationshipIdempotencyManagement
                            .executeIdempotent(followerId, idempotencyKey, differentRequestHash, cb));

            assertEquals(HttpStatus.CONFLICT.value(), exc.getStatusCode().value());
            assertEquals(0, counter.get());
        }
    }

    @Nested
    @DisplayName("FollowRelationshipIdempotencyRecordEntity")
    class FollowRelationshipIdempotencyRecordEntity {

        @Test
        @DisplayName("when idempotency key exceeds column maximum length of 64, throws DataIntegrityViolationException")
        void whenIdempotencyKeyTooLong_throws() {
            UUID authorId = UUID.randomUUID();
            String hash = "a".repeat(64);
            FollowRelationshipRequest entity = new FollowRelationshipRequest(authorId, "k".repeat(65), hash);

            assertThrows(DataIntegrityViolationException.class, () -> repo.saveAndFlush(entity));
        }

        @Test
        @DisplayName("when duplicate primary key (authorId, idempotencyKey) is saved, throws DataIntegrityViolationException")
        void whenDuplicatePrimaryKey_throws() {
            UUID authorId = UUID.randomUUID();
            String key = "a".repeat(32);
            String hash = "b".repeat(64);

            FollowRelationshipRequest entity1 = new FollowRelationshipRequest(authorId, key, hash);
            repo.saveAndFlush(entity1);

            FollowRelationshipRequest entity2 = new FollowRelationshipRequest(authorId, key, hash);
            assertThrows(DataIntegrityViolationException.class, () -> repo.saveAndFlush(entity2));
        }
    }
}
