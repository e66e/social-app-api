package dev.e66e.social_app_api.posts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
class PostController {

    private final PostExternalAPI postAPI;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    PostResponse getPostById(@PathVariable UUID id) {
        log.info("Requested for post with id={}", id);
        return this.postAPI.getPost(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse createPost(@RequestBody CreatePostRequest createPostRequest,
                            @AuthenticationPrincipal Jwt jwt) {
        UUID authorId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        log.info("User with id={} has requested to create a post.", authorId);
        return this.postAPI.createPost(authorId,
                createPostRequest.idempotencyKey(),
                createPostRequest.postRequest());
    }

    @GetMapping("/users/{userId}")
    PostsPageResponse getUserPosts(@PathVariable("userId") UUID userId,
                                   @RequestParam(value = "cursor", required = false) String cursor,
                                   @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.info("Requested for posts from user with id={}.", userId);
        return this.postAPI.getUsersPost(userId, cursor, limit);
    }
}
