package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.posts.PostResponse;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class CommentManagement implements CommentInternalAPI, CommentExternalAPI {

    private final CommentRepository repository;
    private final CommentMapper mapper;

    @Override
    public Optional<CommentResponse> getComment(UUID commentId, UUID postId) {
        Optional<Comment> comment = this.repository.findById(commentId, postId);

        return comment.map(this.mapper::commentToCommentResponse);
    }

    @Override
    public List<CommentResponse> getPostComments(UUID postId, @Nullable String cursor, int limit) {
        CommentCursor cc = CommentCursorCodec.parse(cursor);
        List<dev.e66e.social_app_api.comments.Comment> postComments = this.fetchPostComments(postId, cc, limit);

        return postComments.stream()
                .map(this.mapper::commentToCommentResponse)
                .toList();
    }

    private List<Comment> fetchPostComments(UUID postId, @Nullable CommentCursor cursor, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        if (cursor == null) {
            return this.repository.findAllPostCommentByNewest(postId, pageRequest);
        }
        if (postId != cursor.postId()) {
            throw new IllegalStateException("Cursors' postId and requested postId do not match.");
        }

        return this.repository.findAllPostCommentAfterCursor(postId, cursor.commentId(),
                cursor.postId(), cursor.createdAt(), pageRequest);
    }

    @Override
    public List<CommentResponse> getUserComments(UUID userId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PostResponse postComment(UUID authorId, String idempotencyKey, CommentRequest commentRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
