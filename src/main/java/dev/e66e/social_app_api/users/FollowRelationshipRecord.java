package dev.e66e.social_app_api.users;

import java.util.UUID;

public record FollowRelationshipRecord(
    UUID follower,
    UUID followTarget
) {
}
