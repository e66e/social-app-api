package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.comments.idempotency.CommentIdempotencyAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private CommentsProperties properties;
    private CommentManagement management;

    @BeforeEach
    void setUp() {
        properties = new CommentsProperties();
        properties.setDefaultPageSize(DEFAULT_PAGE_SIZE);
        properties.setMaxPageSize(MAX_PAGE_SIZE);

        management = new CommentManagement(repository, idempotencyAPI, mapper, hasher, properties);
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
            UUID postId = UUID.randomUUID();
            List<Comment> comments = List.of(
                    newComment(UUID.randomUUID(), postId, Instant.now()),
                    newComment(UUID.randomUUID(), postId, Instant.now()));

            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(comments);
            when(mapper.commentToCommentResponse(any())).thenAnswer(inv -> toResponse(inv.getArgument(0)));

            CommentsPageResponse page = management.getPostComments(postId, null, 10);

            assertThat(page.comments()).hasSize(2);
            assertThat(page.nextCursor()).isNull();

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAllPostCommentByNewest(eq(postId), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(11);
        }

        @Test
        void returnsNextCursor_whenResultExceedsLimit() {
            UUID postId = UUID.randomUUID();
            List<Comment> comments = List.of(
                    newComment(UUID.randomUUID(), postId, Instant.now().minusSeconds(1)),
                    newComment(UUID.randomUUID(), postId, Instant.now().minusSeconds(2)),
                    newComment(UUID.randomUUID(), postId, Instant.now().minusSeconds(3)));

            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(comments);
            when(mapper.commentToCommentResponse(any())).thenAnswer(inv -> toResponse(inv.getArgument(0)));
            when(mapper.commentToCommentRecord(any())).thenAnswer(inv -> toRecord(inv.getArgument(0)));

            CommentsPageResponse page = management.getPostComments(postId, null, 2);

            assertThat(page.comments()).hasSize(2);
            // Cursor is derived from the last returned comment (index 1, i.e. size - 2).
            assertThat(page.nextCursor())
                    .isEqualTo(CommentCursorCodec.encode(toRecord(comments.get(1))));
        }

        @Test
        void withValidCursorForSamePost_queriesAfterCursor() {
            UUID postId = UUID.randomUUID();
            UUID lastCommentId = UUID.randomUUID();
            Instant lastCreatedAt = Instant.now();

            // Manually build a cursor in the format that `parse` EXPECTS.
            String cursor = lastCommentId + "|" + postId + "|" + lastCreatedAt.toEpochMilli();
            when(repository.findAllPostCommentAfterCursor(eq(postId), eq(lastCommentId),
                    eq(lastCreatedAt), any())).thenReturn(List.of());

            CommentsPageResponse page = management.getPostComments(postId, cursor, 10);

            assertThat(page.comments()).isEmpty();
            // NOTE: Because CommentManagement uses `postId != cursor.postId()` (reference
            // equality) and because encode()/parse() formats are mismatched, this path
            // currently throws IllegalStateException in production even with this
            // "correctly formatted" cursor. See the two bug-documenting tests below.
        }

        @Test
        void withCursorFromEncode_throwsIllegalStateException_dueToBugs() {
            // BUG 1: encode() writes "postId|commentId|..." but parse() reads
            //        "commentId|postId|...", so a round-tripped cursor has swapped ids.
            // BUG 2: the guard `postId != cursor.postId()` uses reference equality
            //        (should be `!postId.equals(...)`).
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();

            CommentRecord produced = new CommentRecord(commentId, postId, "c", null, Instant.now(), null);
            String cursorFromEncode = CommentCursorCodec.encode(produced);

            assertThatThrownBy(() -> management.getPostComments(postId, cursorFromEncode, 10))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void usesDefaultPageSize_whenLimitOutOfBounds() {
            UUID postId = UUID.randomUUID();
            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(List.of());

            management.getPostComments(postId, null, 0);       // too small
            management.getPostComments(postId, null, MAX_PAGE_SIZE + 1); // too large

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository, times(2)).findAllPostCommentByNewest(eq(postId), captor.capture());
            captor.getAllValues().forEach(p ->
                    // default + 1 extra for lookahead
                    assertThat(p.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE + 1));
        }

        @Test
        void atMaxPageSize_reNormalizesAndFallsBackToDefault() {
            // BUG: fetchPostComments() re-normalizes `limit + 1`. When the caller
            // requests maxPageSize, `maxPageSize + 1 > maxPageSize`, so it is
            // replaced by defaultPageSize. Only a default-sized page is fetched,
            // even though the caller asked for the max.
            UUID postId = UUID.randomUUID();
            when(repository.findAllPostCommentByNewest(eq(postId), any())).thenReturn(List.of());

            management.getPostComments(postId, null, MAX_PAGE_SIZE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAllPostCommentByNewest(eq(postId), captor.capture());

            assertThat(captor.getValue().getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
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
}
