package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.idempotency.AbstractIdempotencyRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "post_creation_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PostCreationRequest extends AbstractIdempotencyRecord {

    PostCreationRequest(UUID authorId, String idempotencyKey, String requestHash) {
        super(authorId, idempotencyKey, requestHash);
    }
}
