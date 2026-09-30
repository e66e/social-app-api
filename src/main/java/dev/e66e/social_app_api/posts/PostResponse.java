package dev.e66e.social_app_api.posts;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        Instant createdAt,
        Instant updatedAt,
        String textContent,
        String imageUrl
) {}
