package dev.e66e.social_app_api.comments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateCommentRequest(
        @NotBlank @Size(min = 32, max = 64) String idempotencyKey,
        @Valid CommentRequest commentRequest
) {}
