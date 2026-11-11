package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.comments.idempotency.CommentIdempotencyAPI;
import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostRecord;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class CommentManagement implements CommentInternalAPI, CommentExternalAPI {

    private final CommentRepository repository;
    private final CommentIdempotencyAPI commentIdempotencyAPI;
    private final CommentMapper mapper;
    private final CommentRequestHasher hasher;
    private final CommentsProperties properties;
    private final PostInternalAPI postAPI;

    @Override
    public Optional<CommentResponse> getComment(UUID commentId, UUID postId) {
        log.info("Getting comment with id {} from post: {}", commentId, postId);
        Optional<Comment> comment = this.repository.findById(commentId, postId);
        log.debug("Found comment: {}", comment);
        return comment.map(this.mapper::commentToCommentResponse);
    }

    @Override
    public CommentsPageResponse getPostComments(UUID postId, @Nullable String cursor, int limit) {
        log.info("Getting post comments for post with id={}", postId);
        log.debug("Passed cursor: {}", cursor);
        log.debug("Limit before normalization: {}", limit);
        int normalizedLimit = this.normalizeLimit(limit);
        log.debug("Limit after normalization: {}", normalizedLimit);
        CommentCursor cc = CommentCursorCodec.parse(cursor);
        List<Comment> postComments = this.fetchPostComments(postId, cc, normalizedLimit + 1);
        log.debug("Fetched {} number of comments", postComments.size());
        return this.createCommentsPageResponse(postComments, normalizedLimit);
    }

    @Override
    public CommentsPageResponse getUserComments(UUID userId, @Nullable String cursor, int limit) {
        log.info("Getting user comments for user with id={}", userId);
        log.debug("Passed cursor: {}", cursor);
        log.debug("Limit before normalization: {}", limit);
        int normalizedLimit = this.normalizeLimit(limit);
        log.debug("Limit after normalization: {}", normalizedLimit);
        CommentCursor cc = CommentCursorCodec.parse(cursor);
        List<Comment> comments = this.fetchUserComments(userId, cc, normalizedLimit + 1);
        log.debug("Fetched {} number of comments", comments.size());
        return this.createCommentsPageResponse(comments, normalizedLimit);
    }

    @Override
    public CommentResponse postComment(UUID authorId, String idempotencyKey, CommentRequest commentRequest) {
        log.info("Data to post comment: authorId={}, idempotencyKey={}, commentRequest={}",
                authorId, idempotencyKey, commentRequest);
        String hash = this.hasher.createCommentHash(authorId, commentRequest);
        log.debug("Comment hash: {}", hash);
        Optional<PostRecord> post = this.postAPI.getPostRecord(commentRequest.postId());

        if (post.isEmpty()) {
            log.error("Post with id={} was not found", commentRequest.postId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post for comment not found.");
        }

        log.debug("Starting idempotency execution.");
        UUID createdCommentId = this.commentIdempotencyAPI.executeIdempotent(authorId, idempotencyKey, hash, () -> {
            Comment newComment = new Comment(
                    commentRequest.postId(),
                    authorId,
                    commentRequest.parentCommentId(),
                    commentRequest.content());

            return this.repository.saveAndFlush(newComment).getId().getId();
        });

        log.debug("Comment created with id={}", createdCommentId);

        return this.getCommentResponse(createdCommentId, commentRequest.postId())
                .orElseThrow(() -> {
                    log.error("Comment with id={} was not found even though it was created earlier.", createdCommentId);
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Comment was created but later on was not found in db.");
                });
    }

    private CommentsPageResponse createCommentsPageResponse(List<Comment> commentsWithExtra, int originalNormalizedLimit) {
        String newCursor = null;

        if (commentsWithExtra.size() > originalNormalizedLimit) {
            CommentRecord cr = this.mapper.commentToCommentRecord(
                    commentsWithExtra.get(commentsWithExtra.size() - 2));
            newCursor = CommentCursorCodec.encode(cr);
        }

        log.debug("New cursor: {}", newCursor);

        return new CommentsPageResponse(
                commentsWithExtra.stream()
                        .map(this.mapper::commentToCommentResponse)
                        .limit(originalNormalizedLimit)
                        .toList(),
                newCursor);
    }

    private int normalizeLimit(int limit) {
        if (limit < 1 || limit > this.properties.getMaxPageSize()) {
            log.debug("Limit is out of range: {}", limit);
            return this.properties.getDefaultPageSize();
        }
        return limit;
    }

    private Optional<CommentResponse> getCommentResponse(UUID commentId, UUID postId) {
        Optional<Comment> comment = this.repository.findById(commentId, postId);
        log.debug("Found comment: {}", comment);
        return comment.map(this.mapper::commentToCommentResponse);
    }

    private List<Comment> fetchPostComments(UUID postId, @Nullable CommentCursor cursor, int normalizedLimit) {
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        if (cursor == null) {
            log.debug("No cursor found for request for comment for post with id={}", postId);
            return this.repository.findAllPostCommentByNewest(postId, pageRequest);
        }
        if (!postId.equals(cursor.postId())) {
            log.debug("Cursor is pointing to different post than was requested, request was for post with id={}", postId);
            throw new IllegalStateException("Cursors' postId and requested postId do not match.");
        }

        return this.repository.findAllPostCommentAfterCursor(postId, cursor.commentId(),
                cursor.createdAt(), pageRequest);
    }

    private List<Comment> fetchUserComments(UUID userId, @Nullable CommentCursor cursor, int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        if (cursor == null) {
            log.debug("Fetched for user comments with no cursor");
            return this.repository.findAllUserCommentByNewest(userId, pageRequest);
        }
        log.debug("Fetched for user comments with cursor={}", cursor);
        return this.repository.findAllUserCommentAfterCursor(userId, cursor.commentId(), cursor.createdAt(), pageRequest);
    }
}
