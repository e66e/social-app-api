package dev.e66e.social_app_api.posts;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID postId,
        @Nullable UUID authorId,
        @Nullable String authorUsername,
        Instant createdAt,
        @Nullable Instant updatedAt,
        String textContent,
        String imageUrl
) {}
