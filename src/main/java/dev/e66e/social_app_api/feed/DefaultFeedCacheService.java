package dev.e66e.social_app_api.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class DefaultFeedCacheService implements FeedCacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final FeedProperties properties;

    @Override
    public Optional<TimelinePage> getHomeFeed(UUID userId) {
        String payload = redis.opsForValue().get(this.homeFeedKey(userId));

        if (payload == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(this.objectMapper.readValue(payload, TimelinePage.class));
        } catch (Exception e) {
            log.info("Something went wrong with redis while fetch for userId {}, {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private String homeFeedKey(UUID userId) {
        return "feed:home" + userId;
    }

    @Override
    public void prependToHomeFeed(UUID userId, FeedItem item) {
        Optional<TimelinePage> cachedFeed = this.getHomeFeed(userId);
        if (cachedFeed.isEmpty()) {
            return;
        }

        TimelinePage existing = cachedFeed.get();

        PriorityQueue<FeedItem> updatedItems = new PriorityQueue<>();
        updatedItems.add(item);

        existing.items().stream()
                .filter(existingItem -> !existingItem.post().postId().equals(item.post().postId()))
                .forEach(updatedItems::add);

        while (updatedItems.size() > properties.getDefaultPageSize()) {
            updatedItems.poll();
        }

        this.writeHomeFeed(new TimelinePage(
                userId,
                updatedItems.size(),
                updatedItems.stream().toList().reversed(),
                existing.totalItems() + 1 > updatedItems.size()
                        && !updatedItems.isEmpty()
                        ? FeedCursorCodec.encode(updatedItems.peek())
                        : null
                )
        );
    }

    @Override
    public void writeHomeFeed(TimelinePage timelinePage) {
        try {
            String payload = this.objectMapper.writeValueAsString(timelinePage);
            this.redis.opsForValue().set(
                    this.homeFeedKey(timelinePage.ownerId()),
                    payload,
                    this.properties.getHomeFeedTtlInMinutes());
        } catch (Exception e) {
            log.info("Something went wrong with redis while setting payload for user {}, {}",
                    timelinePage.ownerId(), e.getMessage());
        }
    }
}
