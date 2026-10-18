package dev.e66e.social_app_api.comments;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record CommentRecord(
        UUID id,
        UUID postId,
        @NotBlank String content,
        @Nullable UUID parentCommentId,
        Instant createdAt,
        @Nullable Instant updatedAt
) {}
