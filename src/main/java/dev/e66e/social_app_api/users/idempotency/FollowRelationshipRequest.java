package dev.e66e.social_app_api.users.idempotency;

import dev.e66e.social_app_api.idempotency.AbstractIdempotencyRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "follow_relationship_creation_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class FollowRelationshipRequest extends AbstractIdempotencyRecord {

    public FollowRelationshipRequest(UUID followerId, String idempotencyKey, String requestHash) {
        super(followerId, idempotencyKey, requestHash);
    }
}
