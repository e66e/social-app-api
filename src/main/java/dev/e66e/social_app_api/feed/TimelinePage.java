package dev.e66e.social_app_api.feed;

import java.util.List;
import java.util.UUID;

public record TimelinePage(
        UUID ownerId,
        int totalItems,
        List<FeedItem> items,
        String nextCursor
) {}
