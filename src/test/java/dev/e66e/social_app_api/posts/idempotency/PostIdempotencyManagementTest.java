package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.posts.PostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostIdempotencyManagementTest {

    @Mock
    PostCreationRequestRepository repo;
    @Mock
    PostIdempotencyManagement self;
    @InjectMocks
    PostIdempotencyManagement postIdempotencyManagement;

    final UUID postId = UUID.randomUUID();
    final UUID authorId = UUID.randomUUID();
    final String idempotencyKey = "idempotencyKeyidempotencyKeyidempotencyKey";
    final String requestHash = "fdsfsfnbvc;bndfbd;bfdb";
    final String textContent = "lorem ipsum";
    final String imageUrl = "";
    final Instant now = Instant.now();

    @Nested
    @DisplayName("executeIdempotent method tests")
    class ExecuteIdempotentMethod {

        @Test
        void whenReservationWins_runsCallbackAndMarksCompleted() {
            // Given

            PostResponse response
                    = new PostResponse(
                            postId,
                            authorId,
                            now,
                            now,
                            textContent,
                            imageUrl);

            AtomicInteger counter = new AtomicInteger(0);
            Supplier<UUID> cb = () -> {
                counter.incrementAndGet();
                return postId;
            };

            PostCreationRequest pcr = new PostCreationRequest(authorId, idempotencyKey, requestHash);
            pcr.setCreatedAt(now);

            when(self.tryReserve(authorId, idempotencyKey, requestHash))
                    .thenReturn(Optional.of(pcr));
            when(repo.saveAndFlush(pcr))
                    .thenAnswer(i -> i.getArgument(0));

            // When
            UUID out = postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(1, counter.get());
            assertEquals(response.id(), out);
            verify(repo).saveAndFlush(argThat(r ->
                    r.getStatus() == PostCreationStatus.COMPLETED));
        }

        @Test
        void whenReservationLoses_andInProgress_Throws409() {
            // Given
            Supplier<UUID> cb = () -> {
                throw new RuntimeException("Test shouldn't access callback in this scenario.");
            };

            PostCreationRequest pcr = new PostCreationRequest(authorId, idempotencyKey, requestHash);

            when(self.tryReserve(authorId, idempotencyKey, requestHash))
                    .thenReturn(Optional.empty());

            when(repo.findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey))
                    .thenReturn(Optional.of(pcr));

            // When / Then
            assertThrows(ResponseStatusException.class, () ->
                postIdempotencyManagement.executeIdempotent(authorId, idempotencyKey, requestHash, cb)
            , "Other thread handles request with given body and idempotency key.");
        }

        @Test
        void whenReservationLoses_andCompleted_returnsCached() {
            // Given
            Supplier<UUID> cb = () -> {
                throw new RuntimeException("Test shouldn't access callback in this scenario.");
            };

            PostCreationRequest pcr = new PostCreationRequest(authorId, idempotencyKey, requestHash);
            pcr.setPostId(postId);
            pcr.setStatus(PostCreationStatus.COMPLETED);

            PostResponse response
                    = new PostResponse(
                        postId,
                        authorId,
                        now,
                        now,
                        textContent,
                        imageUrl);

            when(self.tryReserve(authorId, idempotencyKey, requestHash))
                    .thenReturn(Optional.empty());

            when(repo.findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey))
                    .thenReturn(Optional.of(pcr));

            // When
            UUID out = postIdempotencyManagement
                    .executeIdempotent(authorId, idempotencyKey, requestHash, cb);

            // Then
            assertEquals(response.id(), out);
            verify(repo).findById_authorIdAndId_idempotencyKey(any(), any());
        }
    }

    @Nested
    @DisplayName("tryReserve method tests.")
    class TryReserve {

        @Test
        void whenRaceWon_returnOptionalOfCreatedRecord() {
            // Given
            when(self.reserve(authorId, idempotencyKey, requestHash))
                    .thenReturn(new PostCreationRequest(authorId, idempotencyKey, requestHash));

            // When
            var out = postIdempotencyManagement
                    .tryReserve(authorId, idempotencyKey, requestHash);

            // Then
            assertTrue(out.isPresent());
            assertEquals(PostCreationStatus.IN_PROGRESS, out.get().getStatus());
        }

        @Test
        void whenRaceLost_returnEmptyOptional() {
            // Given
            when(self.reserve(authorId, idempotencyKey, requestHash))
                    .thenThrow(DataIntegrityViolationException.class);

            // When
            var out = postIdempotencyManagement
                    .tryReserve(authorId, idempotencyKey, requestHash);

            // Then
            assertTrue(out.isEmpty());
        }
    }
}