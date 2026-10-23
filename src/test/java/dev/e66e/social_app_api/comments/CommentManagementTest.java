package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.comments.idempotency.CommentIdempotencyAPI;
import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.shadow.de.siegmar.fastcsv.util.Nullable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentManagementTest {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Mock
    private CommentRepository repository;

    @Mock
    private CommentIdempotencyAPI idempotencyAPI;

    @Mock
    private CommentMapper mapper;

    @Mock
    private CommentRequestHasher hasher;

    @Mock
    private PostInternalAPI postAPI;

    private CommentManagement management;

    @BeforeEach
    void setUp() {
        CommentsProperties properties = new CommentsProperties();
        properties.setDefaultPageSize(DEFAULT_PAGE_SIZE);
        properties.setMaxPageSize(MAX_PAGE_SIZE);

        management = new CommentManagement(repository, idempotencyAPI, mapper, hasher, properties, postAPI);
    }

    @Nested
    class GetComment {

        @Test
        void returnsMappedResponse() {
            // Given
            UUID commentId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            Comment comment = newComment(commentId, postId, Instant.now());
            CommentResponse expected = toResponse(comment);

            when(repository.findById(commentId, postId)).thenReturn(Optional.of(comment));
            when(mapper.commentToCommentResponse(comment)).thenReturn(expected);

            // When / Then
            assertThat(management.getComment(commentId, postId)).contains(expected);
        }

        @Test
        void returnsEmpty_whenNotFound() {
            // Given
            when(repository.findById(any(), any())).thenReturn(Optional.empty());

            // When / Then
            assertThat(management.getComment(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    class GetPostComments {

        @Test
        void noCursor_delegatesToByNewest() {
            // Given
            UUID postId = UUID.randomUUID();
            List<Comment> comments = List.of(
                    newComment(UUID.randomUUID(), postId, Instant.now()),
                    newComment(UUID.randomUUID(), postId, Instant.now()));

            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(comments);
            when(mapper.commentToCommentResponse(any())).thenAnswer(inv -> toResponse(inv.getArgument(0)));

            // When
            CommentsPageResponse page = management.getPostComments(postId, null, 10);

            // Then
            assertThat(page.comments()).hasSize(2);
            assertThat(page.nextCursor()).isNull();

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAllPostCommentByNewest(eq(postId), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(11);
        }

        @Test
        void returnsNextCursor_whenResultExceedsLimit() {
            // Given
            UUID postId = UUID.randomUUID();
            List<Comment> comments = List.of(
                    newComment(UUID.randomUUID(), postId, Instant.now().minusSeconds(1)),
                    newComment(UUID.randomUUID(), postId, Instant.now().minusSeconds(2)),
                    newComment(UUID.randomUUID(), postId, Instant.now().minusSeconds(3)));

            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(comments);
            when(mapper.commentToCommentResponse(any())).thenAnswer(inv -> toResponse(inv.getArgument(0)));
            when(mapper.commentToCommentRecord(any())).thenAnswer(inv -> toRecord(inv.getArgument(0)));

            // When
            CommentsPageResponse page = management.getPostComments(postId, null, 2);

            // Then
            assertThat(page.comments()).hasSize(2);

            assertThat(page.nextCursor())
                    .isEqualTo(CommentCursorCodec.encode(toRecord(comments.get(1))));
        }

        @Test
        void withValidCursorForSamePost_queriesAfterCursor() {
            // Given
            UUID postId = UUID.randomUUID();
            UUID lastCommentId = UUID.randomUUID();
            Instant lastCreatedAt = Instant.now();

            // Manually build a cursor in the format that `parse` EXPECTS.
            String cursor = lastCommentId + "|" + postId + "|" + lastCreatedAt.toEpochMilli();
            when(repository.findAllPostCommentAfterCursor(eq(postId), eq(lastCommentId),
                    any(), any())).thenReturn(List.of());

            // When
            CommentsPageResponse page = management.getPostComments(postId, cursor, 10);

            // Then
            assertThat(page.comments()).isEmpty();
        }

        @Test
        void usesDefaultPageSize_whenLimitOutOfBounds() {
            // Given
            UUID postId = UUID.randomUUID();
            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(List.of());

            // When
            management.getPostComments(postId, null, 0);       // too small
            management.getPostComments(postId, null, MAX_PAGE_SIZE + 1); // too large

            // Then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository, times(2)).findAllPostCommentByNewest(eq(postId), captor.capture());
            captor.getAllValues().forEach(p ->
                    // default + 1 extra for lookahead
                    assertThat(p.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE + 1));
        }

        @Test
        void atMaxPageSize_reNormalizesAndFallsBackToDefault() {
            // Given
            UUID postId = UUID.randomUUID();
            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(List.of());

            // When
            management.getPostComments(postId, null, MAX_PAGE_SIZE);

            // Then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAllPostCommentByNewest(eq(postId), captor.capture());

            assertThat(captor.getValue().getPageSize()).isEqualTo(MAX_PAGE_SIZE + 1);
        }
    }

    @Nested
    class GetUserComments {

        @Test
        void noCursor_delegatesToByNewest() {
            // Given
            UUID userId = UUID.randomUUID();
            when(repository.findAllUserCommentByNewest(eq(userId), any())).thenReturn(List.of());

            // When
            CommentsPageResponse page = management.getUserComments(userId, null, 5);

            // Then
            assertThat(page.comments()).isEmpty();
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        void withCursor_queriesAfterCursor() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID lastCommentId = UUID.randomUUID();
            Instant createdAt = Instant.now();
            String cursor = lastCommentId + "|" + UUID.randomUUID() + "|" + createdAt.toEpochMilli();

            when(repository.findAllUserCommentAfterCursor(eq(userId), eq(lastCommentId),
                    any(), any())).thenReturn(List.of());

            // When
            CommentsPageResponse page = management.getUserComments(userId, cursor, 5);

            // Then
            assertThat(page.comments()).isEmpty();
        }
    }

    @Nested
    class PostComment {

        @Test
        void createsThenReadsBack() {
            UUID authorId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            String idempotencyKey = "a".repeat(32);
            String hash = "hashhashhashhashhashhashhashhashhashhashhashhashhashhashhashhash";

            CommentRequest request = new CommentRequest(postId, null, "hello");
            Comment saved = newComment(commentId, postId, Instant.now());
            CommentResponse expected = toResponse(saved);

            when(hasher.createCommentHash(authorId, request)).thenReturn(hash);
            when(idempotencyAPI.executeIdempotent(eq(authorId), eq(idempotencyKey), eq(hash), any()))
                    .thenReturn(commentId);
            when(repository.findById(commentId, postId)).thenReturn(Optional.of(saved));
            when(mapper.commentToCommentResponse(saved)).thenReturn(expected);
            when(postAPI.getPostRecord(postId)).thenReturn(
                    Optional.of(newPostRecord(postId, authorId, null))
            );

            CommentResponse result = management.postComment(authorId, idempotencyKey, request);

            assertThat(result).isEqualTo(expected);
            verify(hasher).createCommentHash(authorId, request);
            verify(postAPI).getPostRecord(eq(postId));
            verify(idempotencyAPI).executeIdempotent(eq(authorId), eq(idempotencyKey), eq(hash), any());
            verify(repository).findById(commentId, postId);
        }

        @Test
        void supplierPersistsComment() {
            UUID authorId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            String idempotencyKey = "a".repeat(32);
            CommentRequest request = new CommentRequest(postId, null, "hello");

            when(hasher.createCommentHash(authorId, request))
                    .thenReturn("hashhashhashhashhashhashhashhashhashhashhashhashhashhashhashhash");
            when(postAPI.getPostRecord(eq(postId))).thenReturn(
                    Optional.of(newPostRecord(postId, authorId, "hello")
            ));
            // Delegate to the supplied callback so we can verify what it persists.
            when(idempotencyAPI.executeIdempotent(any(), any(), any(), any())).thenAnswer(inv -> {
                Supplier<UUID> supplier = inv.getArgument(3);
                return supplier.get();
            });
            when(repository.saveAndFlush(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(new CommentId(commentId, postId));
                return c;
            });
            when(repository.findById(commentId, postId))
                    .thenReturn(Optional.of(newComment(commentId, postId, Instant.now())));
            when(mapper.commentToCommentResponse(any())).thenAnswer(inv ->
                    toResponse(inv.getArgument(0)));

            management.postComment(authorId, idempotencyKey, request);

            ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
            verify(repository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getAuthorId()).isEqualTo(authorId);
            assertThat(saved.getValue().getId().getPostId()).isEqualTo(postId);
            assertThat(saved.getValue().getContent()).isEqualTo("hello");
        }

        @Test
        void throws500_whenCommentDisappearsAfterCreation() {
            UUID authorId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            String idempotencyKey = "a".repeat(32);
            CommentRequest request = new CommentRequest(postId, null, "hello");

            when(hasher.createCommentHash(authorId, request)).thenReturn("hashhashhashhashhashhashhashhashhashhashhashhashhashhashhashhash");
            when(postAPI.getPostRecord(postId)).thenReturn(Optional.of(
                    newPostRecord(postId, authorId, "hello")));
            when(idempotencyAPI.executeIdempotent(any(), any(), any(), any())).thenReturn(commentId);
            when(repository.findById(commentId, postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> management.postComment(authorId, idempotencyKey, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Comment newComment(UUID id, UUID postId, Instant createdAt) {
        Comment c = new Comment(postId, UUID.randomUUID(), null, "hello");
        c.setId(new CommentId(id, postId));
        c.setCreatedAt(createdAt);
        return c;
    }

    private static CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId().getId(), c.getId().getPostId(), c.getContent(),
                c.getParentCommentId(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private static CommentRecord toRecord(Comment c) {
        return new CommentRecord(
                c.getId().getId(), c.getId().getPostId(), c.getContent(),
                c.getParentCommentId(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private static PostRecord newPostRecord(UUID postId, UUID authorId, @Nullable String content) {
        return new PostRecord(
                postId,
                authorId,
                Instant.now().minusSeconds(10),
                null,
                content == null ? "post" : content,
                "");
    }
}
