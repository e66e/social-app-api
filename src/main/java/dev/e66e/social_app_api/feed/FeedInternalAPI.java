package dev.e66e.social_app_api.feed;

import java.util.UUID;

public interface FeedInternalAPI {
    void processPostCreatedEvent(UUID postId);
}
