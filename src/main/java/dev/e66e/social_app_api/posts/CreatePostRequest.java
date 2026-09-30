package dev.e66e.social_app_api.posts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

record CreatePostRequest(
        @NotBlank String idempotencyKey,
        @Valid PostRequest postRequest
) {}
