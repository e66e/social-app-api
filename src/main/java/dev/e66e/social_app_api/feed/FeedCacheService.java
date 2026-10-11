package dev.e66e.social_app_api.feed;


import java.util.Optional;
import java.util.UUID;

interface FeedCacheService {

    Optional<TimelinePage> getHomeFeed(UUID userId);

    void prependToHomeFeed(UUID userId, FeedItem item);

    void writeHomeFeed(TimelinePage timelinePage);
}
