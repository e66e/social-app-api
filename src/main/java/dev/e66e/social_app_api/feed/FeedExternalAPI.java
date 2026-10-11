package dev.e66e.social_app_api.feed;

import java.util.UUID;

public interface FeedExternalAPI {

    TimelinePage getHomeFeed(UUID userId, String cursor, int limit);

    TimelinePage getUserFeed(UUID userId, String cursor, int limit);
}
