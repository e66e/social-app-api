package dev.e66e.social_app_api.posts.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface PostCreationRequestRepository extends JpaRepository<PostCreationRequest, PostCreationRequestId> {

    Optional<PostCreationRequest> findById_authorIdAndId_idempotencyKey(UUID authorId, String idempotencyKey);

    Optional<PostCreationRequest> findByPostId(UUID requestId);
}
