package dev.e66e.social_app_api.idempotency;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
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
class AbstractIdempotencyManagementTest {

    @Mock
    IdempotencyRecordRepository<TestRecord> repo;

    @Mock
    PlatformTransactionManager txManager;

    @Mock
    TransactionStatus txStatus;

    TestManagement subject;

    final UUID authorId = UUID.randomUUID();
    final UUID resourceId = UUID.randomUUID();
    final String idempotencyKey = "idempotencyKeyidempotencyKeyidempotencyKeyidempotencyKey";
    final String requestHash = "requestHashrequestHashrequestHashrequestHashrequestHashrequestHa";

    @BeforeEach
    void setUp() {
        lenient().when(txManager.getTransaction(any(TransactionDefinition.class))).thenReturn(txStatus);
        subject = new TestManagement(repo, txManager);
    }

    @Nested
    class ExecuteIdempotent {
        @Test
        void whenReservationWins_runsCallback_andMarksCompleted() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);
            Supplier<UUID> cb = () -> {
                counter.incrementAndGet();
                return resourceId;
            };

            when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

            // When
            UUID out = subject.executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(resourceId, out);
            assertEquals(1, counter.get());
            // once for reserve, once after markCompleted
            verify(repo, times(2)).saveAndFlush(any());
        }

        @Test
        void whenReservationLoses_andInProgress_throws409() {
            // Given
            when(repo.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);
            when(repo.findByIdempotencyKey(authorId, idempotencyKey))
                    .thenReturn(Optional.of(new TestRecord(authorId, idempotencyKey, requestHash)));

            // When / Then
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> subject.executeIdempotent(authorId, idempotencyKey, requestHash, () -> resourceId));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        void whenReservationLoses_andCompleted_andHashMatches_returnsCached() {
            // Given
            when(repo.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);
            TestRecord existing = new TestRecord(authorId, idempotencyKey, requestHash);
            existing.markCompleted(resourceId);
            when(repo.findByIdempotencyKey(authorId, idempotencyKey)).thenReturn(Optional.of(existing));

            // When
            UUID out = subject.executeIdempotent(authorId, idempotencyKey, requestHash, () -> {
                throw new AssertionError("callback must not run");
            });

            // Then
            assertEquals(resourceId, out);
        }

        @Test
        void whenReservationLoses_andCompleted_andHashDiffers_throws409() {
            // Given
            when(repo.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);
            TestRecord existing = new TestRecord(authorId, idempotencyKey, requestHash);
            existing.markCompleted(resourceId);
            when(repo.findByIdempotencyKey(authorId, idempotencyKey)).thenReturn(Optional.of(existing));

            // Then / When
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> subject.executeIdempotent(authorId, idempotencyKey,
                            "requestHashrequestHashrequestHashrequestHashrequestHashrequestH4", () -> resourceId));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        void whenReservationWins_andCallbackThrows_deletesReservation_andRethrows() {
            // Given
            when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
            when(repo.findByIdempotencyKey(authorId, idempotencyKey))
                    .thenReturn(Optional.of(new TestRecord(authorId, idempotencyKey, requestHash)));

            // When / Then
            assertThrows(RuntimeException.class,
                    () -> subject.executeIdempotent(authorId, idempotencyKey, requestHash, () -> {
                        throw new RuntimeException("boom");
                    }));

            verify(repo).delete(any());
        }
    }

    @Nested
    class TryReserve {

        @Test
        void whenRaceWon_returnsPresent() {
            // Given
            when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

            // When
            var out = subject.tryReserve(authorId, idempotencyKey, requestHash);

            // Then
            assertTrue(out.isPresent());
            assertEquals(IdempotencyStatus.IN_PROGRESS, out.get().getStatus());
        }

        @Test
        void whenRaceLost_returnsEmpty() {
            when(repo.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

            var out = subject.tryReserve(authorId, idempotencyKey, requestHash);

            assertTrue(out.isEmpty());
        }
    }

    // ---- test doubles -----------------------------------------------------------------------------------------------

    static class TestRecord extends AbstractIdempotencyRecord {
        TestRecord(UUID authorId, String key, String hash) {
            super(authorId, key, hash);
        }
    }

    static class TestManagement extends AbstractIdempotencyManagement<TestRecord> {
        TestManagement(IdempotencyRecordRepository<TestRecord> repo, PlatformTransactionManager tx) {
            super(repo, tx, "test");
        }

        @Override
        protected @NonNull TestRecord newRecord(@NonNull UUID authorId, @NonNull String key, @NonNull String hash) {
            return new TestRecord(authorId, key, hash);
        }
    }
}
