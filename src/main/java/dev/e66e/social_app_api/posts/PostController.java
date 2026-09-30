package dev.e66e.social_app_api.posts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
class PostController {

    private final PostExternalAPI postExternalApi;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    PostResponse getPostById(@PathVariable UUID id) {
        return this.postExternalApi.getPost(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse createPost(JwtAuthenticationToken jwt,
                            @RequestBody CreatePostRequest createPostRequest) {
        String subject = jwt.getToken().getSubject();
        if (subject == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "subject is null");

        UUID userId = UUID.fromString(subject);

        return this.postExternalApi.createPost(userId,
                createPostRequest.idempotencyKey(),
                createPostRequest.postRequest());
    }
}
