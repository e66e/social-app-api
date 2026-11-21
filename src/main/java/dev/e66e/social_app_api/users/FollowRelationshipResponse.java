package dev.e66e.social_app_api.users;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record FollowRelationshipResponse(
        @NotBlank UUID followerId,
        @NotBlank String followerUsername,
        @NotBlank UUID followTargetId,
        @NotBlank String followTargetUsername
) {}
