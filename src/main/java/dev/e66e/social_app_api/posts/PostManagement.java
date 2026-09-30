package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.posts.idempotency.PostIdempotencyAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PostManagement implements PostExternalAPI, PostInternalAPI {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostIdempotencyAPI postIdempotencyAPI;

    @Override
    public Optional<PostResponse> getPost(UUID id) {
        Optional<Post> post = postRepository.findById(id);

        return post.map(this.postMapper::postToPostResponse);
    }

    @Override
    @Transactional
    public PostResponse createPost(UUID authorId, String idempotencyKey, PostRequest postRequest) {
        String postHash = this.createPostHash(authorId, postRequest);

        UUID createdPostId = this.postIdempotencyAPI.executeIdempotent(authorId, idempotencyKey, postHash, () -> {
            Post newPost = new Post(authorId, postRequest.content(), postRequest.imageUrl());

            return this.postMapper.postToPostResponse(this.postRepository.save(newPost));
        });

        return this.getPost(createdPostId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Post was created but not found in db."));
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
