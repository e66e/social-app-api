package dev.e66e.social_app_api.posts;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PostInternalAPI {

    Optional<PostRecord> getPostRecord(UUID id);

    List<PostRecord> fetchNewestPostsOfGivenUsers(Set<UUID> userIds, int fetchSize);

    List<PostRecord> fetchNewestPostAfterDateAndId(Set<UUID> userIds, Instant instant, UUID uuid, int fetchSize);

    long getNumberOfPostsOfAuthorGroup(Set<UUID> authorsIds);

    List<PostRecord> getUserNewestPosts(UUID userId, int limit);

    List<PostRecord> getUserPostsAfterCursor(UUID userId, Instant createdAt, UUID postId, int limit);

    long getNumberOfPostsOfAuthor(UUID userId);
}
