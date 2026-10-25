package dev.e66e.social_app_api.users;

import java.util.UUID;

interface FollowRelationshipHasher {

    String createRelationshipHash(UUID followerId, UUID followTargetId);
}
