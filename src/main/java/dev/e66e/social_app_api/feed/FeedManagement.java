package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostPublishedEvent;
import dev.e66e.social_app_api.posts.PostRecord;
import dev.e66e.social_app_api.posts.PostResponse;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
class FeedManagement implements FeedExternalAPI, FeedInternalAPI {

    private final StringRedisTemplate redis;
    private final FeedAsyncProperties asyncProp;
    private final FeedProperties feedProp;
    private final PostInternalAPI postAPI;
    private final UserInternalAPI userAPI;
    private final FeedCacheService feedCacheService;

    @ApplicationModuleListener
    public void onPostPublished(PostPublishedEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("eventId", event.eventId().toString());
        fields.put("postId", event.createdPostId().toString());

        redis.opsForStream().add(MapRecord.create(asyncProp.getStream().getKey(), fields));
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
            log.info("User with id: {} is hot user, therefore no fan out.", author.id());
            return;
        }

        Set<UUID> followers = this.userAPI.getUsersFollowersIds(author.id());

        followers.forEach(followerId -> {
            Set<UUID> followerFollowTargets = new LinkedHashSet<>(this.userAPI.getUsersFollowersIds(followerId));
            FeedItem feedItem = this.toFeedItem(postRecord, followerId, followerFollowTargets);
            this.feedCacheService.prependToHomeFeed(followerId, feedItem);
        });
    }

    // TODO to update
    private FeedItem toFeedItem(PostRecord postRecord, UUID followerId, Set<UUID> followerFollowTargetIds) {
        UserRecord author = this.userAPI.getUser(postRecord.authorId()).orElseThrow(() ->
                new IllegalStateException("Post with id " + postRecord.id() + " with no author."));

        double freshnessWindow = 600.0;
        double recencyScore = 1.0 - Duration
                .between(postRecord.createdAt(), Instant.now()).toMinutes() / freshnessWindow;

        double hotUserPenalty = author.isHotUser() ? 0.15 : 0.0;

        double rankingScore = Math.round((recencyScore - hotUserPenalty) * 100.0) / 100.0;

        return new FeedItem(
                new PostResponse(
                        postRecord.id(),
                        author.id(),
                        author.publicUsername(),
                        postRecord.createdAt(),
                        postRecord.updatedAt(),
                        postRecord.textContent(),
                        postRecord.imageUrl()
                ),
                rankingScore
        );
    }

    @Override
    public TimelinePage getHomeFeed(UUID userId, @Nullable String cursor, int limit) {
        int pageSize = this.normalizeLimit(limit);

        try {
            UserRecord viewer = this.userAPI.getUser(userId).orElseThrow(() ->
                    new IllegalStateException("User with id " + userId + " not found."));
            FeedCursor pageCursor = FeedCursorCodec.parse(cursor);
            Set<UUID> allFollowTargetIds = this.userAPI.getFollowTargetIds(viewer.id());
            GroupedFollowTargets groupedFollowTargets = this.groupFollowTargets(allFollowTargetIds);

            FeedSlice normalFeedSlice = this.getNormalHomeFeedSlice(viewer.id(), groupedFollowTargets.normalUsers(),
                    pageCursor, pageSize);

            FeedSlice hotSlice = getHotHomeFeedSlice(viewer.id(), groupedFollowTargets.hotUsers(),
                    pageCursor, pageSize);

            int totalItems = Math.toIntExact(this.postAPI.getNumberOfPostsOfAuthorGroup(allFollowTargetIds));

            return this.mergeHomeFeedSlices(userId, totalItems, pageSize, normalFeedSlice, hotSlice);
        } catch (ResponseStatusException e) {
            log.error("Error while getting home feed for user: {}\nMessage: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public TimelinePage getUserFeed(UUID userId, @Nullable String cursor, int limit) {
        int pageSize = this.normalizeLimit(limit);

        try {
            Optional<UserRecord> userOp = this.userAPI.getUser(userId);

            if (userOp.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + userId + " not found.");
            }

            FeedCursor pageCursor = FeedCursorCodec.parse(cursor);
            List<PostRecord> posts = this.fetchUserPosts(userId, pageCursor, pageSize + 1);
            int totalItems = Math.toIntExact(this.postAPI.getNumberOfPostsOfAuthor(userId));

            return this.buildTimelinePage(userId, posts, totalItems, pageSize);
        } catch (ResponseStatusException e) {
            log.error("Error while getting user feed for user: {}\nMessage: {}", userId, e.getMessage());
            throw e;
        }
    }

    private TimelinePage buildTimelinePage(UUID ownerId, List<PostRecord> posts,
                                           int totalItems, int pageSize) {
        boolean hasMore = posts.size() > pageSize;
        List<PostRecord> pagePosts = hasMore ? posts.subList(0, pageSize) : posts;

        List<FeedItem> pageItems = pagePosts.stream()
                .map(post -> this.toFeedItem(post, ownerId, Set.of(ownerId)))
                .toList();

        String nextCursor = hasMore && !pageItems.isEmpty()
                ? FeedCursorCodec.encode(pageItems.getLast())
                : null;

        return new TimelinePage(ownerId, totalItems, pageItems, nextCursor);
    }

    private List<PostRecord> fetchUserPosts(UUID userId, @Nullable FeedCursor pageCursor, int limit) {
        if (pageCursor == null) {
            return this.postAPI.getUserNewestPosts(userId, limit);
        }
        return this.postAPI.getUserPostsAfterCursor(userId, pageCursor.createdAt(), pageCursor.postId(), limit);
    }

    private TimelinePage mergeHomeFeedSlices(UUID userId, int totalItems, int pageSize,
                                             FeedSlice normalFeedSlice, FeedSlice hotSlice) {
        List<FeedItem> mergedItems = Stream.concat(normalFeedSlice.items().stream(), hotSlice.items().stream())
                .sorted(Comparator.reverseOrder())
                .limit(pageSize + 1)
                .collect(Collectors.toCollection(ArrayList::new));

        boolean hasMore = mergedItems.size() > pageSize
                || normalFeedSlice.hasMore()
                || hotSlice.hasMore();

        if (mergedItems.size() > pageSize) {
            mergedItems.removeLast();
        }

        String nextCursor = hasMore && !mergedItems.isEmpty()
                ? FeedCursorCodec.encode(mergedItems.getLast())
                : null;

        return new TimelinePage(
                userId,
                totalItems,
                mergedItems,
                nextCursor
        );
    }

    private FeedSlice getHotHomeFeedSlice(UUID userId, Set<UUID> hotUsersIds,
                                          @Nullable FeedCursor cursor, int fetchSize) {
        if (hotUsersIds.isEmpty()) {
            return new FeedSlice(List.of(), false);
        }

        List<PostRecord> posts = this.fetchHomeFeedPosts(hotUsersIds, cursor, fetchSize + 1);

        return this.buildFeedSlice(posts, fetchSize, userId, hotUsersIds);
    }

    private FeedSlice getNormalHomeFeedSlice(UUID userId, Set<UUID> normalUserIds,
                                             @Nullable FeedCursor pageCursor, int pageSize) {
        if (normalUserIds.isEmpty()) {
            return new FeedSlice(List.of(), false);
        }

        Optional<FeedSlice> cachedSlice = this.getCachedNormalHomeFeedSlice(userId, pageCursor, pageSize);

        if (cachedSlice.isPresent()) {
            return cachedSlice.get();
        }

        List<PostRecord> posts = this.fetchHomeFeedPosts(normalUserIds, pageCursor, pageSize + 1);

        FeedSlice slice = this.buildFeedSlice(posts, pageSize, userId, normalUserIds);

        if (pageCursor == null && pageSize == this.feedProp.getDefaultPageSize()) {
            TimelinePage tp = new TimelinePage(
                    userId,
                    Math.toIntExact(this.postAPI.getNumberOfPostsOfAuthorGroup(normalUserIds)),
                    slice.items(),
                    slice.hasMore() && !slice.items().isEmpty()
                            ? FeedCursorCodec.encode(slice.items().getLast())
                            : null
            );

            this.feedCacheService.writeHomeFeed(tp);
        }

        return slice;
    }

    private FeedSlice buildFeedSlice(List<PostRecord> posts, int pageSize, UUID followerId, Set<UUID> followTargetsIds) {
        boolean hasMore = posts.size() > pageSize;
        List<PostRecord> pagePosts = hasMore
                ? posts.subList(0, pageSize)
                : posts;

        List<FeedItem> pageItems = pagePosts.stream()
                .map(post -> this.toFeedItem(post, followerId, followTargetsIds))
                .toList();

        return new FeedSlice(pageItems, hasMore);
    }

    private List<PostRecord> fetchHomeFeedPosts(Set<UUID> users, @Nullable FeedCursor pageCursor,
                                                int fetchSize) {
        if (pageCursor == null) {
            return this.postAPI.fetchNewestPostsOfGivenUsers(users, fetchSize);
        }
        return this.postAPI.fetchNewestPostAfterDateAndId(users, pageCursor.createdAt(),
                pageCursor.postId(), fetchSize);
    }

    private Optional<FeedSlice> getCachedNormalHomeFeedSlice(UUID userId, @Nullable FeedCursor cursor, int pageSize) {
        if (cursor != null) {
            return Optional.empty();
        }

        if (pageSize > feedProp.getDefaultPageSize()) {
            return Optional.empty();
        }

        Optional<TimelinePage> cachedPage = this.feedCacheService.getHomeFeed(userId);

        return cachedPage.flatMap(timelinePage -> this.adaptCachedFirstPage(timelinePage, pageSize));
    }

    private Optional<FeedSlice> adaptCachedFirstPage(TimelinePage cachedPage, int pageSize) {
        if (cachedPage.items().size() < pageSize && cachedPage.nextCursor() != null) {
            return Optional.empty();
        }

        List<FeedItem> pageItems = cachedPage.items().stream().limit(pageSize).toList();

        return Optional.of(new FeedSlice(pageItems, cachedPage.totalItems() > pageItems.size()));
    }

    private GroupedFollowTargets groupFollowTargets(Set<UUID> allFollowTargets) {
        Set<UserRecord> userRecords = this.userAPI.getUserGroup(allFollowTargets);
        Set<UUID> hotUsers = new LinkedHashSet<>();
        Set<UUID> normalUsers = new LinkedHashSet<>();
        userRecords.forEach(followTarget -> {
            if (followTarget.isHotUser()) {
                hotUsers.add(followTarget.id());
            } else {
                normalUsers.add(followTarget.id());
            }
        });
        return new GroupedFollowTargets(hotUsers, normalUsers);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return this.feedProp.getDefaultPageSize();
        }

        return Math.min(limit, this.feedProp.getMaxPageSize());
    }
}

record GroupedFollowTargets(
        Set<UUID> hotUsers,
        Set<UUID> normalUsers
) {}
