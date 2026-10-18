package dev.e66e.social_app_api.comments.idempotency;

import dev.e66e.social_app_api.idempotency.AbstractIdempotencyRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "comment_creation_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CommentCreationRequest extends AbstractIdempotencyRecord {

    CommentCreationRequest(UUID authorId, String idempotencyKey, String requestHash) {
        super(authorId, idempotencyKey, requestHash);
    }
}
