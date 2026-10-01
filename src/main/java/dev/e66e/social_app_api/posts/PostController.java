package dev.e66e.social_app_api.posts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
class PostController {

    private final PostExternalAPI postExternalApi;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    PostResponse getPostById(@PathVariable UUID id) {
        return this.postExternalApi.getPost(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse createPost(@RequestBody CreatePostRequest createPostRequest,
                            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        return this.postExternalApi.createPost(userId,
                createPostRequest.idempotencyKey(),
                createPostRequest.postRequest());
    }
}
