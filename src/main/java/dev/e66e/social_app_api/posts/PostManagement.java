package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.posts.idempotency.PostIdempotencyAPI;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
class PostManagement implements PostExternalAPI, PostInternalAPI {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostIdempotencyAPI postIdempotencyAPI;
    private final UserInternalAPI userAPI;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<PostResponse> getPost(UUID id) {
        Optional<Post> postOp = postRepository.findById(id);

        if (postOp.isEmpty()) {
            return Optional.empty();
        }

        Post post = postOp.get();

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
        // TODO try to add agent which will check if post contents are harmful
        String postHash = this.createPostHash(authorId, postRequest);

        UUID createdPostId = this.postIdempotencyAPI.executeIdempotent(authorId, idempotencyKey, postHash, () -> {
            Post newPost = new Post(authorId, postRequest.content(),
                    Objects.requireNonNull(postRequest.imageUrl()));

            return this.postRepository.saveAndFlush(newPost).getId();
        });

        PostRecord post = this.getPostRecord(createdPostId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Post was created but later on was not found in db."));

        String authorUsername = this.userAPI.getUser(authorId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "User was authenticated but was not found in db."))
                .publicUsername();

        this.eventPublisher.publishEvent(new PostPublishedEvent(UUID.randomUUID(), createdPostId));

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
    public Optional<PostRecord> getPostRecord(UUID id) {
        Optional<Post> post = this.postRepository.findById(id);

        return post.map(postMapper::postToPostRecord);
    }

    @Override
    public List<PostRecord> fetchNewestPostsOfGivenUsers(Set<UUID> userIds, int fetchSize) {
        PageRequest pageRequest = PageRequest.of(0, fetchSize);
        List<Post> posts = this.postRepository.findByAuthorIdInOrderByCreatedAtDescIdDesc(userIds, pageRequest);

        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public List<PostRecord> fetchNewestPostAfterDateAndId(Set<UUID> userIds, Instant createdAt,
                                                          UUID postId, int fetchSize) {
        PageRequest pageRequest = PageRequest.of(0, fetchSize);
        List<Post> posts = this.postRepository
                .findHomePageByCreatedAtAndPostId(userIds, createdAt, postId, pageRequest);

        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public long getNumberOfPostsOfAuthorGroup(Set<UUID> authorsIds) {
        return this.postRepository.countByAuthorIdIn(authorsIds);
    }

    @Override
    public List<PostRecord> getUserNewestPosts(UUID userId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Post> posts = this.postRepository.findByAuthorIdOrderByCreatedAtDescIdDesc(userId, pageRequest);

        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public List<PostRecord> getUserPostsAfterCursor(UUID userId, Instant createdAt, UUID postId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Post> posts = this.postRepository.findByCursor(userId, createdAt, postId, pageRequest);

        return posts.stream()
                .map(this.postMapper::postToPostRecord)
                .toList();
    }

    @Override
    public long getNumberOfPostsOfAuthor(UUID userId) {
        return this.postRepository.countByAuthorId(userId);
    }

    private String createPostHash(UUID authorId, PostRequest postRequest) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((authorId
                + "\n" + postRequest.content()
                + "\n" + postRequest.imageUrl())
                .getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hash);
    }
}
