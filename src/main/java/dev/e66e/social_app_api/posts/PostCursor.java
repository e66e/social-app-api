package dev.e66e.social_app_api.posts;

import java.time.Instant;
import java.util.UUID;

record PostCursor(
        UUID postId,
        Instant createdAt
) {}
