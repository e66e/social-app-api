package dev.e66e.social_app_api.users.idempotency;

import dev.e66e.social_app_api.idempotency.IdempotencyRecordRepository;
import org.springframework.stereotype.Repository;

@Repository
interface FollowRelationshipRequestRepository extends IdempotencyRecordRepository<FollowRelationshipRequest> {
}
