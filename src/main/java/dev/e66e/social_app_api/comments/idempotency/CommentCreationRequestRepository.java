package dev.e66e.social_app_api.comments.idempotency;

import dev.e66e.social_app_api.idempotency.IdempotencyRecordRepository;
import org.springframework.stereotype.Repository;

@Repository
interface CommentCreationRequestRepository extends IdempotencyRecordRepository<CommentCreationRequest> {
}
