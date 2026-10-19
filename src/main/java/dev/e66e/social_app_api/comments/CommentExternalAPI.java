package dev.e66e.social_app_api.comments;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Optional;
import java.util.UUID;

interface CommentExternalAPI {

    Optional<CommentResponse> getComment(UUID commentId, UUID postId);

    CommentsPageResponse getPostComments(UUID postId, @Nullable String cursor, int limit);

    CommentsPageResponse getUserComments(UUID userId, @Nullable String cursor, int limit);

    CommentResponse postComment(UUID authorId,
                             @NotBlank @Size(min = 32, max = 64)
                             String idempotencyKey,
                             CommentRequest commentRequest);
}
