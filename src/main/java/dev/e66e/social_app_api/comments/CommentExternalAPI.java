package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.posts.PostResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

interface CommentExternalAPI {

    PostResponse postComment(UUID userId, @NotBlank @Size(min = 32, max = 64) String idempotencyKey, CommentRequest commentRequest);

    List<CommentResponse> getPostComments(UUID postId);

    List<CommentResponse> getUserComments(UUID userId);
}
