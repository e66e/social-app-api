package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostPublishedEvent;
import dev.e66e.social_app_api.posts.PostRecord;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
class FeedManagement implements FeedExternalAPI, FeedInternalAPI {

    private final StringRedisTemplate redis;
    private final FeedAsyncProperties prop;
    private final PostInternalAPI postAPI;
    private final UserInternalAPI userAPI;
    private final FeedCacheService feedCacheService;

    @ApplicationModuleListener
    public void onPostPublished(PostPublishedEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("eventId", event.eventId().toString());
        fields.put("postId", event.createdPostId().toString());

        redis.opsForStream().add(MapRecord.create(prop.getStream().getKey(), fields));
    }

    @Override
    public void processPostCreatedEvent(UUID postId) {
        PostRecord postRecord = this.postAPI.getPostRecord(postId)
                .orElseThrow(() ->
                        new IllegalStateException("Post was not found, even though its' id was in PostPublishedEvent.")
                );

        this.applyDeliveryStrategy(postRecord);
    }

    private void applyDeliveryStrategy(PostRecord postRecord) {
        UserRecord author = this.userAPI.getUser(postRecord.authorId()).orElseThrow(() ->
                new IllegalStateException("Post with no author."));

        if (author.isHotUser()) {
            log.info("User with id: {} is hot user, therefore no fan out.",
                    author.id());
            return;
        }

        List<UserRecord> followers = this.userAPI.getUsersFollowers(author.id());

        followers.forEach(follower -> {
            Set<UserRecord> followerFollowTargets = new LinkedHashSet<>(this.userAPI.getUsersFollowers(follower.id()));
            FeedItem feedItem = this.toFeedItem(follower, followerFollowTargets);
            this.feedCacheService.prependToHomeFeed(follower.id(), feedItem);
        });
    }

    private FeedItem toFeedItem(UserRecord follower, Set<UserRecord> followerFollowTargets) {

    }
}
