package dev.e66e.social_app_api.feed;

import java.time.Instant;
import java.util.UUID;

record FeedCursor(
        Instant createdAt,
        UUID postId
) {}
