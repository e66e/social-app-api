package dev.e66e.social_app_api.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        return "feed:home:" + userId;
    }

    @Override
    public void prependToHomeFeed(UUID userId, FeedItem item) {
        log.info("Prepending feedItem={} for userId:{}", item, userId);
        Optional<TimelinePage> cachedFeed = this.getHomeFeed(userId);
        if (cachedFeed.isEmpty()) {
            log.info("Inactive user, skipping prepending to feed.");
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
        log.info("Prepending item={} for user with id={} was success.", item, userId);
    }

    @Override
    public void writeHomeFeed(TimelinePage timelinePage) {
        log.info("Writing home feed of user with id={}.", timelinePage.ownerId());
        long longDuration = this.redis.getExpire(this.homeFeedKey(timelinePage.ownerId()), TimeUnit.MINUTES);

        if (longDuration == -1) {
            log.error("Timeout for timelinePage of user with id={} was set to infinite.", timelinePage.ownerId());
            throw new IllegalStateException("Invalid timeout for timelinePage of user with id=" + timelinePage.ownerId());
        }

        Duration duration = longDuration > 0
                ? Duration.ofMinutes(longDuration)
                : Duration.ofMinutes(this.properties.getHomeFeedTtlInMinutes());

        try {
            String payload = this.objectMapper.writeValueAsString(timelinePage);
            this.redis.opsForValue().set(
                    this.homeFeedKey(timelinePage.ownerId()),
                    payload,
                    Expiration.from(duration)
            );
            log.info("Timeline page for user with id={} was cached successfully.", timelinePage.ownerId());
        } catch (Exception e) {
            log.error("Something went wrong with redis while setting payload for user {}, {}",
                    timelinePage.ownerId(), e.getMessage());
        }
    }
}
