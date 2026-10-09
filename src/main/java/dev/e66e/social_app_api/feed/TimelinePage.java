package dev.e66e.social_app_api.feed;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

public record TimelinePage(
        UUID ownerId,
        int totalItems,
        List<FeedItem> items,
        @Nullable String nextCursor
) {}
