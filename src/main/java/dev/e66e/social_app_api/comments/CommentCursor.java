package dev.e66e.social_app_api.comments;

import java.time.Instant;
import java.util.UUID;

record CommentCursor(
        UUID commentId,
        UUID postId,
        Instant createdAt
) {}
