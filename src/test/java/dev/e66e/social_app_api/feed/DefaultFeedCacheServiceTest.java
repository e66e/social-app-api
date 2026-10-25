package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.types.Expiration;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultFeedCacheServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FeedProperties properties;

    @InjectMocks
    private DefaultFeedCacheService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(properties.getDefaultPageSize()).thenReturn(10);
        lenient().when(properties.getHomeFeedTtlInMinutes()).thenReturn(60);
    }

    @Nested
    class GetHomeFeedTests {

        @Test
        void returnsEmptyWhenRedisHasNoValue() {
            // Given
            UUID userId = UUID.randomUUID();
            when(valueOps.get("feed:home" + userId)).thenReturn(null);

            // When
            Optional<TimelinePage> result = service.getHomeFeed(userId);


            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void returnsDeserializedPage() {
            // Given
            UUID userId = UUID.randomUUID();
            TimelinePage page = new TimelinePage(userId, 1, List.of(), null);

            when(valueOps.get("feed:home" + userId)).thenReturn("json");
            when(objectMapper.readValue("json", TimelinePage.class)).thenReturn(page);

            // When
            Optional<TimelinePage> result = service.getHomeFeed(userId);

            // Then
            assertThat(result).contains(page);
        }

        @Test
        void returnsEmptyWhenDeserializationFails() {
            // Given
            UUID userId = UUID.randomUUID();

            when(valueOps.get("feed:home" + userId)).thenReturn("json");
            when(objectMapper.readValue("json", TimelinePage.class))
                    .thenThrow(new RuntimeException("bad json"));

            // When
            Optional<TimelinePage> result = service.getHomeFeed(userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WriteHomeFeedTests {

        @Test
        void serializesAndSetsWithTtl() {
            // Given
            UUID userId = UUID.randomUUID();
            TimelinePage page = new TimelinePage(userId, 0, List.of(), null);

            when(objectMapper.writeValueAsString(page)).thenReturn("{\"x\":1}");

            // When
            service.writeHomeFeed(page);

            // Then
            verify(valueOps).set(
                    eq("feed:home" + userId),
                    eq("{\"x\":1}"),
                    eq(Expiration.from(Duration.ofMinutes(60)))
            );
        }
    }

    @Nested
    class PrependToHomeFeedTests {

        @Test
        void updatesCachedPageAndTrimsToPageSize_AndIncludesNewBestItems() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            UUID newPostId = UUID.randomUUID();

            when(properties.getDefaultPageSize()).thenReturn(2);

            TimelinePage existing = new TimelinePage(
                    userId,
                    2,
                    List.of(feedItem(postId1, 1.0), feedItem(postId2, 0.9)),
                    null
            );

            when(valueOps.get("feed:home" + userId)).thenReturn("json");
            when(objectMapper.readValue("json", TimelinePage.class)).thenReturn(existing);
            when(objectMapper.writeValueAsString(any(TimelinePage.class))).thenReturn("{}");

            // When
            service.prependToHomeFeed(userId, feedItem(newPostId, 0.95));

            // Then
            ArgumentCaptor<TimelinePage> captor = ArgumentCaptor.forClass(TimelinePage.class);
            verify(objectMapper).writeValueAsString(captor.capture());

            TimelinePage written = captor.getValue();

            assertThat(written.ownerId()).isEqualTo(userId);
            assertThat(written.items()).hasSize(2);
            assertThat(written.items())
                    .extracting(item -> item.post().postId())
                    .contains(newPostId);
            assertThat(written.totalItems()).isEqualTo(2);
        }

        @Test
        void updatesCachedPageAndTrimsToPageSize_AndRejectsNewWorstItem() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            UUID newPostId = UUID.randomUUID();

            when(properties.getDefaultPageSize()).thenReturn(2);

            TimelinePage existing = new TimelinePage(
                    userId,
                    2,
                    List.of(feedItem(postId1, 1.0), feedItem(postId2, 0.9)),
                    null
            );

            when(valueOps.get("feed:home" + userId)).thenReturn("json");
            when(objectMapper.readValue("json", TimelinePage.class)).thenReturn(existing);
            when(objectMapper.writeValueAsString(any(TimelinePage.class))).thenReturn("{}");

            // When
            service.prependToHomeFeed(userId, feedItem(newPostId, 0.8));

            // Then
            ArgumentCaptor<TimelinePage> captor = ArgumentCaptor.forClass(TimelinePage.class);
            verify(objectMapper).writeValueAsString(captor.capture());

            TimelinePage written = captor.getValue();

            assertThat(written.ownerId()).isEqualTo(userId);
            assertThat(written.items()).hasSize(2);
            assertThat(written.items())
                    .extracting(item -> item.post().postId())
                    .doesNotContain(newPostId);
            assertThat(written.totalItems()).isEqualTo(2);
        }
    }

    private FeedItem feedItem(UUID postId, double score) {
        Instant now = Instant.now();

        return new FeedItem(
                new PostResponse(
                        postId,
                        UUID.randomUUID(),
                        "user",
                        now,
                        now,
                        "text",
                        null
                ),
                score
        );
    }
}