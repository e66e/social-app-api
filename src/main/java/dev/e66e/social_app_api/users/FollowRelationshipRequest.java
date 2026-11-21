package dev.e66e.social_app_api.users;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record FollowRelationshipRequest(
        @NotNull String followTargetUsername,
        @Size(min = 32, max = 64) String idempotencyKey
) {}
