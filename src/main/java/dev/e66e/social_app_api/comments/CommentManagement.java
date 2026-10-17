package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.CommentRepository;
import dev.e66e.social_app_api.posts.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class CommentManagement implements CommentInternalAPI, CommentExternalAPI {

    private final CommentRepository repository;

    @Override
    public PostResponse postComment(UUID authorId, String idempotencyKey, CommentRequest commentRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CommentResponse> getPostComments(UUID postId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CommentResponse> getUserComments(UUID userId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
