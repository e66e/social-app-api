package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles({"test"})
@PostgresTestcontainers
@Import(PostIdempotencyManagement.class)
class PostIdempotencyManagementIT {

    @MockitoBean
    PostInternalAPI postAPI;

    @Autowired
    PostCreationRequestRepository repo;

    @Autowired
    PostIdempotencyManagement postIdempotencyManagement;

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class TryReserve {

        final UUID authorId = UUID.randomUUID();
        final String idempotencyKey = "idempotencyKeyidempotencyKeyidempotencyKey";
        final String requestHash = "requestHashrequestHashrequestHashrequestHash";

        @AfterEach
        void cleanup() {
            repo.deleteAll();
        }

        @Test
        void whenNoDuplication_ReturnOptionalOfCreatedRecord() {
            // Given

            // When
            var out = postIdempotencyManagement
                    .tryReserve(authorId, idempotencyKey, requestHash);

            // Then
            assertTrue(out.isPresent());
            assertEquals(PostCreationStatus.IN_PROGRESS, out.get().getStatus());
        }

        @Test
        void whenRecordDuplicated_ReturnEmptyOptional() {
            // Given
            repo.saveAndFlush(new PostCreationRequest(authorId, idempotencyKey, requestHash));

            // When
            var out = postIdempotencyManagement
                    .tryReserve(authorId, idempotencyKey, requestHash);

            // Then
            assertTrue(out.isEmpty());

            // Check if data was pushed to db
            var existing = repo
                    .findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey);

            assertTrue(existing.isPresent());
            assertEquals(PostCreationStatus.IN_PROGRESS, existing.get().getStatus());
        }
    }

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class ExecuteIdempotent {

        final UUID authorId = UUID.randomUUID();
        final UUID postId = UUID.randomUUID();
        final String requestHash = "requestHashrequestHashrequestHashrequestHash";
        final String idempotencyKey = "idempotencyKeyidempotencyKeyidempotencyKey";
        final Instant now = Instant.now();
        final String content = "lorem ipsum";
        final PostResponse postResponse
                = new PostResponse(
                postId,
                authorId,
                now,
                now,
                content,
                ""
        );
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
            PostCreationRequest idempotencyRecord = repo.findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey)
                    .orElseThrow(() -> new RuntimeException("Post Idempotent failed"));

            assertEquals(PostCreationStatus.COMPLETED, idempotencyRecord.getStatus());
            assertEquals(postId, idempotencyRecord.getPostId());
            assertEquals(authorId, idempotencyRecord.getId().getAuthorId());
            assertEquals(idempotencyKey, idempotencyRecord.getId().getIdempotencyKey());
            assertEquals(requestHash, idempotencyRecord.getRequestHash());
            assertNotNull(idempotencyRecord.getCreatedAt());
            assertNotNull(idempotencyRecord.getUpdatedAt());
            assertNotNull(out);
            assertEquals(1, counter.get());
        }

        @Test
        void whenRaceWon_andThereWasProblemWithPersistingPost_thenRemoveIdempotencyRecordAndThrowException() {
            // Given
            Supplier<UUID> cb = () -> {
                throw new RuntimeException("Simulating database error");
            };

            // When / Then
            assertThrows(RuntimeException.class, () -> postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb),
                    "Simulating database error");

            assertTrue(repo.findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey)
                           .isEmpty());
        }

        @Test
        void whenRaceLost_thenReturnCompletedPost() {
            // Given
            PostCreationRequest entity = new PostCreationRequest(authorId, idempotencyKey, requestHash);
            entity.setPostId(postId);
            entity.setStatus(PostCreationStatus.COMPLETED);
            repo.saveAndFlush(entity);

            when(postAPI.getPost(postId))
                    .thenReturn(Optional.of(postResponse));

            // When
            UUID out = postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(postResponse.id(), out);

            assertEquals(0, counter.get());
        }

        @Test
        void whenRaceLost_AndThereWasRecordInProgress_thenThrowExceptionWithConflictStatusCode() {
            // Given
            PostCreationRequest entity = new PostCreationRequest(authorId, idempotencyKey, requestHash);
            repo.saveAndFlush(entity);

            // When / Then
            ResponseStatusException exc = assertThrows(ResponseStatusException.class,
                    () -> postIdempotencyManagement
                        .executeIdempotent(authorId, idempotencyKey, requestHash, cb));

            assertEquals(HttpStatus.CONFLICT.value(), exc.getStatusCode().value());
            String excMessage = exc.getMessage();
            assertEquals("Other thread handles request with given body and idempotency key.",
                    excMessage.substring(excMessage.indexOf('"') + 1,
                            excMessage.lastIndexOf('"'))
            );
        }
    }
}
