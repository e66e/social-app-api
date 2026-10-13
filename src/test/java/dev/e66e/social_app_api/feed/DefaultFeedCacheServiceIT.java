package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.RedisTestcontainersInitializer;
import dev.e66e.social_app_api.posts.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataRedisTest
@Import(DefaultFeedCacheService.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ModuleSlicing
@ContextConfiguration(initializers = RedisTestcontainersInitializer.class)
public class DefaultFeedCacheServiceIT {

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    FeedProperties feedProp;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private FeedCacheService cacheService;

    @BeforeEach
    void flushRedis() {
        RedisConnectionFactory connectionFactory = redis.getConnectionFactory();
        if (connectionFactory == null) {
            throw new RuntimeException("RedisConnection is null");
        }
        connectionFactory.getConnection().serverCommands().flushAll();
    }

    @Test
    void writeThenGet_roundTripsThroughRedis() {
        // Given
        UUID userId = UUID.randomUUID();
        TimelinePage page = new TimelinePage(
                userId,
                42,
                List.of(feedItem(UUID.randomUUID(), "alice", 1.0)),
                "1700000000000|" + UUID.randomUUID()
        );

        // When
        cacheService.writeHomeFeed(page);

        // Then
        Optional<TimelinePage> loaded = cacheService.getHomeFeed(userId);

        assertThat(loaded).isPresent();
        TimelinePage actual = loaded.get();
        assertThat(actual.ownerId()).isEqualTo(userId);
        assertThat(actual.totalItems()).isEqualTo(42);
        assertThat(actual.items()).hasSize(1);
        assertThat(actual.items().getFirst().post().authorPubUsername()).isEqualTo("alice");
        assertThat(actual.nextCursor()).isEqualTo(page.nextCursor());
    }

    @Test
    void write_setTtlOnKey() {
        // Given
        UUID userId = UUID.randomUUID();
        when(feedProp.getHomeFeedTtlInMinutes()).thenReturn(60);

        // When
        cacheService.writeHomeFeed(new TimelinePage(userId, 0, List.of(), null));

        // Then
        Long ttlSeconds = redis.getExpire("feed:home" + userId);
        assertThat(ttlSeconds).isNotNull().isBetween(1L, 60L * 60L);
    }

    @Test
    void getHomeFeed_missingKey_returnsEmpty() {
        // When / Then
        assertThat(cacheService.getHomeFeed(UUID.randomUUID())).isEmpty();
    }

    @Test
    void prependToHomeFeed_noCache_doesNothing() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        cacheService.prependToHomeFeed(userId, feedItem(UUID.randomUUID(), "bob", 0.50));

        // Then
        assertThat(redis.hasKey("feed:home" + userId)).isFalse();
    }

    @Test
    void prependToHomeFeed_trimsToPageSizeAndKeepsNewest() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID old1 = UUID.randomUUID();
        UUID old2 = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();

        TimelinePage existing = new TimelinePage(
                userId,
                2,
                List.of(feedItem(old1, "alice", 1.0),
                        feedItem(old2, "alice", 0.3)),
                null
        );

        when(feedProp.getDefaultPageSize()).thenReturn(2);

        cacheService.writeHomeFeed(existing);

        // When
        cacheService.prependToHomeFeed(userId, feedItem(fresh, "alice", 0.7));

        // Then
        TimelinePage reloaded = cacheService.getHomeFeed(userId).orElseThrow();

        assertThat(reloaded.items()).extracting(i -> i.post().postId()).contains(fresh);
        assertThat(reloaded.items()).hasSize(feedProp.getDefaultPageSize());
    }

    private FeedItem feedItem(UUID postId, String username, double score) {
        Instant now = Instant.now();
        return new FeedItem(
                new PostResponse(
                        postId,
                        UUID.randomUUID(),
                        username,
                        now,
                        null,
                        "text",
                        null
                ),
                score
        );
    }
}
