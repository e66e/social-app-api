package dev.e66e.social_app_api.posts;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        Instant createdAt,
        @Nullable Instant updatedAt,
        String textContent,
        String imageUrl
) {}
