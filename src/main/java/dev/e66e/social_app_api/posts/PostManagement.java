package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.posts.idempotency.PostIdempotencyAPI;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
class PostManagement implements PostExternalAPI, PostInternalAPI {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostIdempotencyAPI postIdempotencyAPI;
    private final UserInternalAPI userAPI;
    private final ApplicationEventPublisher eventPublisher;
    private final PostRequestHasher hasher;
    private final PostsProperties properties;

    @Override
    public Optional<PostResponse> getPost(UUID id) {
        log.info("Getting post with id={}.", id);
        Optional<Post> postOp = postRepository.findById(id);

        if (postOp.isEmpty()) {
            log.info("Post with id={} not found.", id);
            return Optional.empty();
        }

        Post post = postOp.get();
        log.info("Post with id={} found.", id);

        Optional<UserRecord> userOp = userAPI.getUser(post.getAuthorId());

        return Optional.of(new PostResponse(
                post.getId(),
                userOp.map(UserRecord::id).orElse(null),
                userOp.map(UserRecord::publicUsername).orElse(null),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getTextContent(),
                post.getImageUrl()
        ));
    }

    @Override
    @Transactional
    public PostResponse createPost(UUID authorId, String idempotencyKey, PostRequest postRequest) {
        log.info("Creating post by user with id={}, idempotencyKey={}.", authorId, idempotencyKey);
        String postHash = this.hasher.createPostHash(authorId, postRequest);

        UUID createdPostId = this.postIdempotencyAPI.executeIdempotent(authorId, idempotencyKey, postHash, () -> {
            Post newPost = new Post(authorId, postRequest.content(),
                    Objects.requireNonNull(postRequest.imageUrl()));

            return this.postRepository.saveAndFlush(newPost).getId();
        });

        PostRecord post = this.getPostRecord(createdPostId).orElseThrow(() -> {
            log.error("Post with id={} was created but later on was not found.", createdPostId);
            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Post was created but later on was not found in db.");
        });

        String authorUsername = this.userAPI.getUser(authorId)
                .orElseThrow(() -> {
                    log.error("Author with id={} passed verification but later on was not found.", authorId);
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "User was authenticated but was not found in db.");
                })
                .publicUsername();

        this.eventPublisher.publishEvent(new PostPublishedEvent(UUID.randomUUID(), createdPostId));

        log.info("Post was created with id={}.", createdPostId);
        return new PostResponse(
                createdPostId,
                authorId,
                authorUsername,
                post.createdAt(),
                post.updatedAt(),
                post.textContent(),
                post.imageUrl()
        );
    }

    @Override
    public PostsPageResponse getUsersPost(UUID userId,
                                          @Nullable String cursor,
                                          int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        log.info("Getting posts from user with id={}, with limit={}.", userId, normalizedLimit);
        PostCursor pc = PostCursorCodec.parse(cursor);
        List<Post> postsWithExtra = this.getUserNewestPosts(userId, pc, normalizedLimit + 1);
        String newCursor = null;

        if (postsWithExtra.isEmpty()) {
            log.info("User was not found or has no posts");
            return new PostsPageResponse(List.of(), null);
        }

        if (postsWithExtra.size() > normalizedLimit) {
            PostRecord pr = this.postMapper.postToPostRecord(
                    postsWithExtra.get(postsWithExtra.size() - 2));
            newCursor = PostCursorCodec.encode(pr);
        }

        String authorPubUsername = this.userAPI.getUser(userId).orElseThrow(() -> {
                log.error("User with id={} was not found, even though it has posts.", userId);
                return new IllegalStateException("User was not found, even though it has posts.");
            }).publicUsername();

        List<PostResponse> postResponses = postsWithExtra.stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getAuthorId(),
                        authorPubUsername,
                        post.getCreatedAt(),
                        post.getUpdatedAt(),
                        post.getTextContent(),
                        post.getImageUrl()
                )).limit(normalizedLimit).toList();

        log.info("Found {} posts from user with id={}.", postResponses.size(), userId);
        return new PostsPageResponse(
                postResponses,
                newCursor);
    }

    @Override
    public Optional<PostRecord> getPostRecord(UUID id) {
        Optional<Post> post = this.postRepository.findById(id);

        return post.map(postMapper::postToPostRecord);
    }

    @Override
    public List<PostRecord> fetchNewestPostsOfGivenUsers(Set<UUID> userIds, int fetchSize) {
        log.info("Fetching newest posts from {} number of users.", userIds.size());
        int normalizedLimit = this.normalizeLimit(fetchSize);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        List<Post> posts = this.postRepository.findByAuthorIdInOrderByCreatedAtDescIdDesc(userIds, pageRequest);

        log.info("Found {} posts from {} number of users by getting newest.", posts.size(), userIds.size());
        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public List<PostRecord> fetchNewestPostAfterDateAndId(Set<UUID> userIds, Instant createdAt,
                                                          UUID postId, int fetchSize) {
        log.info("Fetching newest posts by cursor, timestamp={}, postId={}, fetchSize={}.",
                createdAt, postId, fetchSize);
        int normalizedLimit = this.normalizeLimit(fetchSize);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        List<Post> posts = this.postRepository
                .findHomePageByCreatedAtAndPostId(userIds, createdAt, postId, pageRequest);

        log.info("Found {} posts from {} number of users by searching with cursor.",
                posts.size(), userIds.size());
        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public long getNumberOfPostsOfAuthorGroup(Set<UUID> authorsIds) {
        log.info("Getting number of posts from {} number of users.", authorsIds.size());
        return this.postRepository.countByAuthorIdIn(authorsIds);
    }

    @Override
    public List<PostRecord> getUserNewestPosts(UUID userId, int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        log.info("Getting newest posts from user with id={}.", userId);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        List<Post> posts = this.postRepository.findByAuthorIdOrderByCreatedAtDescIdDesc(userId, pageRequest);
        log.info("Found {} posts from user with id={} with searching by newest.", posts.size(), userId);
        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public List<PostRecord> getUserPostsAfterCursor(UUID userId, Instant createdAt, UUID postId, int limit) {
        log.info("Getting posts after cursor: createdAt={}, postId={}, from user with id={}",
                createdAt, postId, userId);
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Post> posts = this.postRepository.findByCursor(userId, createdAt, postId, pageRequest);

        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public long getNumberOfPostsOfAuthor(UUID userId) {
        log.info("Getting number of posts of user with id={}.", userId);
        return this.postRepository.countByAuthorId(userId);
    }

    private List<Post> getUserNewestPosts(UUID userId,
                                          @Nullable PostCursor cursor,
                                          int limit) {
        int normalizedLimit = this.normalizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, normalizedLimit);
        if (cursor == null) {
            log.info("Getting newest posts from user with id={}.", userId);
            return this.postRepository.findByAuthorIdOrderByCreatedAtDescIdDesc(userId, pageRequest);
        }
        log.info("Getting users posts after cursor.");
        return this.postRepository.findByCursor(userId, cursor.createdAt(), cursor.postId(), pageRequest);
    }

    private int normalizeLimit(int limit) {
        if (limit < 1 || limit > this.properties.getMaxPageSize()) {
            return this.properties.getDefaultPageSize();
        }
        return limit;
    }
}
