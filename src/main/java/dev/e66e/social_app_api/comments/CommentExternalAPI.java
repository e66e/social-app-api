package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.posts.PostResponse;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CommentExternalAPI {

    Optional<CommentResponse> getComment(UUID commentId, UUID postId);

    List<CommentResponse> getPostComments(UUID postId, @Nullable String cursor, int limit);

    List<CommentResponse> getUserComments(UUID userId);

    PostResponse postComment(UUID userId,
                             @NotBlank @Size(min = 32, max = 64)
                        String idempotencyKey,
                             CommentRequest commentRequest);
}
