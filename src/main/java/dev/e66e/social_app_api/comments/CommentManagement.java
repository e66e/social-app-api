package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.comments.idempotency.CommentIdempotencyAPI;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class CommentManagement implements CommentInternalAPI, CommentExternalAPI {

    private final CommentRepository repository;
    private final CommentIdempotencyAPI commentIdempotencyAPI;
    private final CommentMapper mapper;
    private final CommentRequestHasher hasher;
    private final CommentsProperties properties;

    @Override
    public Optional<CommentResponse> getComment(UUID commentId, UUID postId) {
        Optional<Comment> comment = this.repository.findById(commentId, postId);

        return comment.map(this.mapper::commentToCommentResponse);
    }

    @Override
    public CommentsPageResponse getPostComments(UUID postId, @Nullable String cursor, int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        CommentCursor cc = CommentCursorCodec.parse(cursor);
        List<Comment> postComments = this.fetchPostComments(postId, cc, normalizedLimit + 1);
        return this.createCommentsPageResponse(postComments, normalizedLimit);
    }

    @Override
    public CommentsPageResponse getUserComments(UUID userId, @Nullable String cursor, int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        CommentCursor cc = CommentCursorCodec.parse(cursor);
        List<Comment> comments = this.fetchUserComments(userId, cc, normalizedLimit + 1);

        return this.createCommentsPageResponse(comments, normalizedLimit);
    }

    private CommentsPageResponse createCommentsPageResponse(List<Comment> commentsWithExtra, int originalLimit) {
        String newCursor = null;
        int normalizedLimit = this.normalizeLimit(originalLimit);

        if (commentsWithExtra.size() > normalizedLimit) {
            CommentRecord cr = this.mapper.commentToCommentRecord(
                    commentsWithExtra.get(commentsWithExtra.size() - 2));
            newCursor = CommentCursorCodec.encode(cr);
        }

        return new CommentsPageResponse(
                commentsWithExtra.stream()
                        .map(this.mapper::commentToCommentResponse)
                        .limit(normalizedLimit)
                        .toList(),
                newCursor
        );
    }

    private int normalizeLimit(int limit) {
        if (limit < 1 || limit > this.properties.getMaxPageSize()) {
            return this.properties.getDefaultPageSize();
        }
        return limit;
    }

    @Override
    public CommentResponse postComment(UUID authorId, String idempotencyKey, CommentRequest commentRequest) {
        String hash = this.hasher.createCommentHash(authorId, commentRequest);

        UUID createdCommentId = this.commentIdempotencyAPI.executeIdempotent(authorId, idempotencyKey, hash, () -> {
            Comment newComment = new Comment(
                    commentRequest.postId(),
                    authorId,
                    commentRequest.parentCommentId(),
                    commentRequest.content());

            return this.repository.saveAndFlush(newComment).getId().getId();
        });

        return this.getCommentResponse(createdCommentId, commentRequest.postId())
                .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Comment was created but later on was not found in db."));
    }

    private Optional<CommentResponse> getCommentResponse(UUID commentId, UUID postId) {
        Optional<Comment> comment = this.repository.findById(commentId, postId);

        return comment.map(this.mapper::commentToCommentResponse);
    }

    private List<Comment> fetchPostComments(UUID postId, @Nullable CommentCursor cursor, int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        if (cursor == null) {
            return this.repository.findAllPostCommentByNewest(postId, pageRequest);
        }
        if (postId != cursor.postId()) {
            throw new IllegalStateException("Cursors' postId and requested postId do not match.");
        }

        return this.repository.findAllPostCommentAfterCursor(postId, cursor.commentId(),
                cursor.createdAt(), pageRequest);
    }

    private List<Comment> fetchUserComments(UUID userId, @Nullable CommentCursor cursor, int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        if (cursor == null) {
            return this.repository.findAllUserCommentByNewest(userId, pageRequest);
        }
        return this.repository.findAllUserCommentAfterCursor(userId, cursor.commentId(), cursor.createdAt(), pageRequest);
    }
}
