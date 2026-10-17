package dev.e66e.social_app_api.comments;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

record CommentRequest(
        UUID postId,
        @Nullable UUID parentCommentId,
        @NotBlank String content
) {}
