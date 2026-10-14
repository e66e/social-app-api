package dev.e66e.social_app_api.feed;

import jakarta.annotation.Nullable;

import java.util.UUID;

public interface FeedExternalAPI {

    TimelinePage getHomeFeed(UUID userId, @Nullable String cursor, int limit);

    TimelinePage getUserFeed(UUID userId, String cursor, int limit);
}
