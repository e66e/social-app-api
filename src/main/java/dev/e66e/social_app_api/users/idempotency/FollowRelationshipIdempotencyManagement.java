package dev.e66e.social_app_api.users.idempotency;

import dev.e66e.social_app_api.idempotency.AbstractIdempotencyManagement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

@Service
class FollowRelationshipIdempotencyManagement extends AbstractIdempotencyManagement<FollowRelationshipRequest>
        implements FollowRelationshipIdempotencyAPI {

    public FollowRelationshipIdempotencyManagement(FollowRelationshipRequestRepository repository,
                                                   PlatformTransactionManager txManager) {
        super(repository, txManager, "followRelationship");
    }

    @Override
    protected FollowRelationshipRequest newRecord(UUID authorId, String idempotencyKey, String requestHash) {
        return new FollowRelationshipRequest(authorId, idempotencyKey, requestHash);
    }
}
