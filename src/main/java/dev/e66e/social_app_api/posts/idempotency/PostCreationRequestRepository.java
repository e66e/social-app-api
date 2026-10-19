package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.idempotency.IdempotencyRecordRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PostCreationRequestRepository extends IdempotencyRecordRepository<PostCreationRequest> {
}
