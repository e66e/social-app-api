package dev.e66e.social_app_api.comments.idempotency;

import dev.e66e.social_app_api.idempotency.AbstractIdempotencyManagement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

@Service
class CommentIdempotencyManagement extends AbstractIdempotencyManagement<CommentCreationRequest> implements CommentIdempotencyAPI {

    protected CommentIdempotencyManagement(CommentCreationRequestRepository repository,
                                           PlatformTransactionManager txManager) {
        super(repository, txManager, "comment");
    }

    @Override
    protected CommentCreationRequest newRecord(UUID authorId, String idempotencyKey, String requestHash) {
        return new CommentCreationRequest(authorId, idempotencyKey, requestHash);
    }
}
