package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostRecord;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedManagementTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private FeedAsyncProperties asyncProp;

    @Mock
    private FeedProperties feedProp;

    @Mock
    private PostInternalAPI postAPI;

    @Mock
    private UserInternalAPI userAPI;

    @Mock
    private FeedCacheService feedCacheService;

    @InjectMocks
    private FeedManagement feedManagement;

    @Nested
    class ProcessPostCreatedEventTests {

        @Test
        void hotAuthor_doesNotFanOut() {
            // Given
            UUID postId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();

            PostRecord post = mock(PostRecord.class);
            UserRecord author = mock(UserRecord.class);

            when(postAPI.getPostRecord(postId)).thenReturn(Optional.of(post));
            when(post.authorId()).thenReturn(authorId);
            when(userAPI.getUser(authorId)).thenReturn(Optional.of(author));
            when(author.isHotUser()).thenReturn(true);

            // When
            feedManagement.processPostCreatedEvent(postId);

            // Then
            verify(userAPI, never()).getUsersFollowersIds(any());
            verifyNoInteractions(feedCacheService);
        }

        @Test
        void normalAuthor_fansOutToFollowers() {
            // Given
            UUID postId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID followerId = UUID.randomUUID();
            Instant now = Instant.now();

            PostRecord post = new PostRecord(
                    postId,
                    authorId,
                    now,
                    null,
                    "hello",
                    "http://img"
            );

            UserRecord author = new UserRecord(
                    authorId,
                    "alice",
                    "alice",
                    null,
                    null,
                    true,
                    UserRole.USER,
                    false
            );

            when(postAPI.getPostRecord(postId)).thenReturn(Optional.of(post));
            when(userAPI.getUser(authorId)).thenReturn(Optional.of(author));
            when(userAPI.getUsersFollowersIds(authorId)).thenReturn(Set.of(followerId));
            when(userAPI.getUsersFollowersIds(followerId)).thenReturn(Set.of(authorId));

            // When
            feedManagement.processPostCreatedEvent(postId);

            // Then
            ArgumentCaptor<FeedItem> captor = ArgumentCaptor.forClass(FeedItem.class);
            verify(feedCacheService).prependToHomeFeed(eq(followerId), captor.capture());

            FeedItem item = captor.getValue();
            assertThat(item.post().postId()).isEqualTo(postId);
            assertThat(item.post().authorId()).isNotNull().isEqualTo(authorId);
            assertThat(item.post().authorPubUsername()).isEqualTo("alice");
            assertThat(item.post().textContent()).isEqualTo("hello");
            assertThat(item.post().imageUrl()).isEqualTo("http://img");
            assertThat(item.rankingScore()).isEqualTo(1.0);
        }
    }
}