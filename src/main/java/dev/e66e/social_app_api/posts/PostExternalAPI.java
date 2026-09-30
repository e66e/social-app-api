package dev.e66e.social_app_api.posts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;
import java.util.UUID;

interface PostExternalAPI {

    Optional<PostResponse> getPost(UUID id);
    PostResponse createPost(UUID userId, @NotBlank String s, @Valid PostRequest postRequest);
}
