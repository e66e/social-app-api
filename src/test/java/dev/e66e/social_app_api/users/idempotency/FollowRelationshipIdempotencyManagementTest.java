package dev.e66e.social_app_api.users.idempotency;

import dev.e66e.social_app_api.idempotency.IdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowRelationshipIdempotencyManagement Unit Tests")
class FollowRelationshipIdempotencyManagementTest {

    @Mock
    FollowRelationshipRequestRepository repo;

    @Mock
    PlatformTransactionManager txManager;

    @Mock
    TransactionStatus txStatus;

    FollowRelationshipIdempotencyManagement subject;

    final UUID followerId = UUID.randomUUID();
    final UUID followTargetId = UUID.randomUUID();
    final String idempotencyKey = "a".repeat(32);
    final String requestHash = "b".repeat(64);

    @BeforeEach
    void setUp() {
        lenient().when(txManager.getTransaction(any(TransactionDefinition.class))).thenReturn(txStatus);
        subject = new FollowRelationshipIdempotencyManagement(repo, txManager);
    }

    @Test
    @DisplayName("newRecord should instantiate FollowRelationshipRequest with followerId, key, and hash")
    void newRecord_createsFollowRelationshipRequest() {
        FollowRelationshipRequest record = subject.newRecord(followerId, idempotencyKey, requestHash);

        assertNotNull(record);
        assertEquals(followerId, record.getAuthorId());
        assertEquals(idempotencyKey, record.getIdempotencyKey());
        assertEquals(requestHash, record.getRequestHash());
        assertEquals(IdempotencyStatus.IN_PROGRESS, record.getStatus());
    }

    @Nested
    @DisplayName("executeIdempotent")
    class ExecuteIdempotent {

        @Test
        @DisplayName("when reservation wins, runs callback and marks record completed")
        void whenReservationWins_runsCallback_andMarksCompleted() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);
            Supplier<UUID> cb = () -> {
                counter.incrementAndGet();
                return followTargetId;
            };

            when(repo.saveAndFlush(any(FollowRelationshipRequest.class))).thenAnswer(i -> i.getArgument(0));

            // When
            UUID result = subject.executeIdempotent(followerId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(followTargetId, result);
            assertEquals(1, counter.get());
            // Once for reservation (IN_PROGRESS), once after markCompleted (COMPLETED)
            verify(repo, times(2)).saveAndFlush(any(FollowRelationshipRequest.class));
        }

        @Test
        @DisplayName("when reservation wins and callback throws, removes reservation and rethrows")
        void whenReservationWins_andCallbackThrows_deletesReservation_andRethrows() {
            // Given
            FollowRelationshipRequest reservedRecord = new FollowRelationshipRequest(followerId, idempotencyKey, requestHash);
            when(repo.saveAndFlush(any(FollowRelationshipRequest.class))).thenAnswer(i -> i.getArgument(0));
            when(repo.findByIdempotencyKey(followerId, idempotencyKey)).thenReturn(Optional.of(reservedRecord));

            Supplier<UUID> cb = () -> {
                throw new IllegalStateException("Business logic failure");
            };

            // When / Then
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> subject.executeIdempotent(followerId, idempotencyKey, requestHash, cb));
            assertEquals("Business logic failure", ex.getMessage());

            verify(repo).delete(reservedRecord);
        }

        @Test
        @DisplayName("when reservation loses and record is in progress, throws 409 Conflict")
        void whenReservationLoses_andInProgress_throws409Conflict() {
            // Given
            FollowRelationshipRequest existingInProgress = new FollowRelationshipRequest(followerId, idempotencyKey, requestHash);
            when(repo.saveAndFlush(any(FollowRelationshipRequest.class))).thenThrow(DataIntegrityViolationException.class);
            when(repo.findByIdempotencyKey(followerId, idempotencyKey)).thenReturn(Optional.of(existingInProgress));

            // When / Then
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> subject.executeIdempotent(followerId, idempotencyKey, requestHash, () -> followTargetId));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        @DisplayName("when reservation loses, completed and hash matches, returns cached target ID without running callback")
        void whenReservationLoses_andCompleted_andHashMatches_returnsCachedTargetId() {
            // Given
            FollowRelationshipRequest completedRecord = new FollowRelationshipRequest(followerId, idempotencyKey, requestHash);
            completedRecord.setResourceId(followTargetId);
            completedRecord.setStatus(IdempotencyStatus.COMPLETED);

            when(repo.saveAndFlush(any(FollowRelationshipRequest.class))).thenThrow(DataIntegrityViolationException.class);
            when(repo.findByIdempotencyKey(followerId, idempotencyKey)).thenReturn(Optional.of(completedRecord));

            // When
            UUID result = subject.executeIdempotent(followerId, idempotencyKey, requestHash, () -> {
                fail("Callback must not be executed when returning cached result");
                return null;
            });

            // Then
            assertEquals(followTargetId, result);
        }

        @Test
        @DisplayName("when reservation loses, completed but hash differs, throws 409 Conflict")
        void whenReservationLoses_andCompleted_andHashDiffers_throws409Conflict() {
            // Given
            FollowRelationshipRequest completedRecord = new FollowRelationshipRequest(followerId, idempotencyKey, requestHash);
            completedRecord.setResourceId(followTargetId);
            completedRecord.setStatus(IdempotencyStatus.COMPLETED);

            when(repo.saveAndFlush(any(FollowRelationshipRequest.class))).thenThrow(DataIntegrityViolationException.class);
            when(repo.findByIdempotencyKey(followerId, idempotencyKey)).thenReturn(Optional.of(completedRecord));

            String differentRequestHash = "c".repeat(64);

            // When / Then
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> subject.executeIdempotent(followerId, idempotencyKey, differentRequestHash, () -> followTargetId));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        @DisplayName("when reservation loses but record cannot be found, throws 500 Internal Server Error")
        void whenReservationLoses_andRecordNotFound_throws500() {
            // Given
            when(repo.saveAndFlush(any(FollowRelationshipRequest.class))).thenThrow(DataIntegrityViolationException.class);
            when(repo.findByIdempotencyKey(followerId, idempotencyKey)).thenReturn(Optional.empty());

            // When / Then
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> subject.executeIdempotent(followerId, idempotencyKey, requestHash, () -> followTargetId));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        }
    }
}
