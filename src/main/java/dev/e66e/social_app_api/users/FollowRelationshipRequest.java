package dev.e66e.social_app_api.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record FollowRelationshipRequest(
        @NotBlank UUID followTargetId,
        @Size(min = 32, max = 64) String idempotencyKey
) {}
