package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.RedisTestcontainersInitializer;
import dev.e66e.social_app_api.posts.PostResponse;
import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostRecord;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataRedisTest
@ContextConfiguration(initializers = RedisTestcontainersInitializer.class)
@FeedTestProperties
@Import({
        FeedManagement.class,
        FeedManagementIT.PropertiesConfig.class,
        DefaultFeedCacheService.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
public class FeedManagementIT {

    @Autowired
    FeedExternalAPI feedAPI;

    @Autowired
    StringRedisTemplate redis;

    @MockitoBean
    PostInternalAPI postAPI;

    @MockitoBean
    UserInternalAPI userAPI;

    @TestConfiguration
    @EnableConfigurationProperties({FeedAsyncProperties.class, FeedProperties.class})
    static class PropertiesConfig {}

    @BeforeEach
    void flushRedis() {
        RedisConnectionFactory conn = redis.getConnectionFactory();
        if (conn == null) {
            throw new RuntimeException("RedisConnectionFactory is null");
        }

        conn.getConnection().serverCommands().flushAll();
    }

    @Test
    void getHomeFeed_noCache_populatesCache() {
        // Given
        UUID viewer = UUID.randomUUID();
        UUID followed = UUID.randomUUID();
        UserRecord viewerRecord = user(viewer, false);
        UserRecord followedRecord = user(followed, false);

        when(userAPI.getUser(viewer)).thenReturn(Optional.of(viewerRecord));
        when(userAPI.getUser(followed)).thenReturn(Optional.of(followedRecord));
        when(userAPI.getFollowTargetIds(viewer)).thenReturn(Set.of(followed));
        when(userAPI.getUserGroup(Set.of(followed))).thenReturn(Set.of(followedRecord));

        PostRecord post = post(followed, Instant.now());
        when(postAPI.fetchNewestPostsOfGivenUsers(eq(Set.of(followed)), anyInt()))
                .thenReturn(List.of(post));
        when(postAPI.getNumberOfPostsOfAuthorGroup(any())).thenReturn(1L);

        // When
        TimelinePage page = feedAPI.getHomeFeed(viewer, null, 10);

        // Then
        assertThat(page.items()).isNotNull();
        assertThat(page.items()).hasSize(1);
        assertThat(Objects.requireNonNull(page.items().getFirst().post().authorId())).isEqualTo(followed);
        assertThat(redis.hasKey("feed:home" + viewer)).isTrue();
    }

    @Test
    void getHomeFeed_withCache_doesNotHitPostAPIForFirstPage() {
        // Given
        UUID viewer = UUID.randomUUID();
        UUID followed = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        FeedItem cachedItem = new FeedItem(
                new PostResponse(
                        postId, followed, "bob",
                        Instant.now(), Instant.now(), "cached", null
                ),
                1.0
        );
        redis.opsForValue().set(
                "feed:home" + viewer,
                new tools.jackson.databind.ObjectMapper().writeValueAsString(
                        new TimelinePage(viewer, 1, List.of(cachedItem), null)),
                java.time.Duration.ofMinutes(60)
        );

        when(userAPI.getUser(viewer)).thenReturn(Optional.of(user(viewer, false)));
        when(userAPI.getFollowTargetIds(viewer)).thenReturn(Set.of(followed));
        when(userAPI.getUserGroup(Set.of(followed))).thenReturn(Set.of(user(followed, false)));
        when(postAPI.getNumberOfPostsOfAuthorGroup(any())).thenReturn(1L);

        // When
        TimelinePage page = feedAPI.getHomeFeed(viewer, null, 10);

        // Then
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().post().postId()).isEqualTo(postId);

        verify(postAPI, org.mockito.Mockito.never())
                .fetchNewestPostsOfGivenUsers(any(), anyInt());
        verify(postAPI, org.mockito.Mockito.never())
                .fetchNewestPostAfterDateAndId(any(), any(), any(), anyInt());
    }

    @Test
    void getHomeFeed_normalAndHotUsers_areMergedAndSortedByScore() {
        // Given
        UUID viewerId = UUID.randomUUID();
        UUID normalId = UUID.randomUUID();
        UUID hotId = UUID.randomUUID();
        UserRecord viewer = user(viewerId, false);
        UserRecord normalUser = user(normalId, false);
        UserRecord hotUser = user(hotId, true);

        when(userAPI.getUser(viewerId)).thenReturn(Optional.of(viewer));
        when(userAPI.getUser(normalId)).thenReturn(Optional.of(normalUser));
        when(userAPI.getUser(hotId)).thenReturn(Optional.of(hotUser));
        when(userAPI.getFollowTargetIds(viewerId)).thenReturn(Set.of(normalId, hotId));
        when(userAPI.getUserGroup(Set.of(normalId, hotId)))
                .thenReturn(Set.of(normalUser, hotUser));

        PostRecord normalPost = post(normalId, Instant.now());
        System.out.println("normalPost: " + normalPost.id());
        PostRecord hotPost = post(hotId, Instant.now());
        System.out.println("hotPost: " + hotPost.id());

        when(postAPI.fetchNewestPostsOfGivenUsers(eq(Set.of(normalId)), anyInt()))
                .thenReturn(List.of(normalPost));
        when(postAPI.fetchNewestPostsOfGivenUsers(eq(Set.of(hotId)), anyInt()))
                .thenReturn(List.of(hotPost));
        when(postAPI.getNumberOfPostsOfAuthorGroup(any())).thenReturn(2L);

        // When
        TimelinePage page = feedAPI.getHomeFeed(viewerId, null, 10);

        // Then
        assertThat(page.items()).hasSize(2);
        assertThat(Objects.requireNonNull(page.items().getFirst().post().authorId())).isEqualTo(normalId);
    }

    @Test
    void getUserFeed_paginatesWithCursor() {
        UUID owner = UUID.randomUUID();
        Instant base = Instant.now();

        when(userAPI.getUser(owner)).thenReturn(Optional.of(user(owner, false)));
        when(postAPI.getNumberOfPostsOfAuthor(owner)).thenReturn(25L);

        List<PostRecord> firstPage = List.of(
                post(owner, base),
                post(owner, base.minusSeconds(10)),
                post(owner, base.minusSeconds(20))
        );
        when(postAPI.getUserNewestPosts(eq(owner), eq(3))).thenReturn(firstPage);

        TimelinePage page = feedAPI.getUserFeed(owner, null, 2);

        assertThat(page.items()).hasSize(2);
        assertThat(page.nextCursor()).isNotNull();
        assertThat(page.totalItems()).isEqualTo(25);

        // Follow-up request uses the cursor and hits the "after cursor" path.
        Instant cursorTime = page.items().get(1).post().createdAt().truncatedTo(ChronoUnit.MILLIS);
        UUID cursorPostId = page.items().get(1).post().postId();
        when(postAPI.getUserPostsAfterCursor(eq(owner), eq(cursorTime), eq(cursorPostId), eq(3)))
                .thenReturn(List.of(post(owner, base.minusSeconds(30))));

        TimelinePage next = feedAPI.getUserFeed(owner, page.nextCursor(), 2);

        verify(postAPI).getUserPostsAfterCursor(eq(owner), eq(cursorTime), eq(cursorPostId), eq(3));

        assertThat(next.items()).hasSize(1);
        assertThat(next.nextCursor()).isNull();
    }

    // ---- helpers ----------------------------------------------------------------------------------------------------

    private static PostRecord post(UUID authorId, Instant createdAt) {
        //noinspection DataFlowIssue
        return new PostRecord(
                UUID.randomUUID(),
                authorId,
                createdAt,
                null,
                "hello",
                null
        );
    }

    private static UserRecord user(UUID userId, boolean hot) {
        return new UserRecord(
                userId,
                "user-"+userId,
                "user-"+userId,
                null,
                null,
                true,
                UserRole.USER,
                hot
        );
    }
}
