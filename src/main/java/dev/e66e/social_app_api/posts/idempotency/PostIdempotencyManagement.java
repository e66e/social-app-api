package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.idempotency.AbstractIdempotencyManagement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

@Service
class PostIdempotencyManagement extends AbstractIdempotencyManagement<PostCreationRequest> implements PostIdempotencyAPI {

    PostIdempotencyManagement(PostCreationRequestRepository repository, PlatformTransactionManager txManager) {
        super(repository, txManager, "post");
    }

    @Override
    protected PostCreationRequest newRecord(UUID authorId, String idempotencyKey, String requestHash) {
        return new PostCreationRequest(authorId, idempotencyKey, requestHash);
    }
}
