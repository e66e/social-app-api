package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.posts.PostResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
class PostIdempotencyManagement implements PostIdempotencyAPI {

    private final PostCreationRequestRepository postCreationRequestRepository;
    private final PostIdempotencyManagement self;

    public PostIdempotencyManagement(PostCreationRequestRepository postCreationRequestRepository,
                                     @Lazy PostIdempotencyManagement self) {
        this.postCreationRequestRepository = postCreationRequestRepository;
        this.self = self;
    }

    // TODO maybe this should return Optional of PostResponse? this way we would break circular dependency
    // and we wont need to use postAPI
    @Override
    @Transactional
    public UUID executeIdempotent(UUID authorId,
                                          String idempotencyKey,
                                          String requestHash,
                                          Supplier<PostResponse> callback) {

        var pcqOpt = self.tryReserve(authorId, idempotencyKey, requestHash);
        if (pcqOpt.isEmpty()) {
            PostCreationRequest pcr
                    = this.postCreationRequestRepository.findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Request exists but was not found.")
                    );


            return switch (pcr.getStatus()) {
                case PostCreationStatus.IN_PROGRESS -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Other thread handles request with given body and idempotency key.");

                case PostCreationStatus.COMPLETED -> pcr.getPostId();
            };
        }

        PostCreationRequest pcr = pcqOpt.get();

        try {
            PostResponse postResponse = callback.get();
            pcr.markCompleted(postResponse.id());
            this.postCreationRequestRepository.saveAndFlush(pcr);

            return pcr.getPostId();
        } catch (RuntimeException e) {
            self.deleteReservation(authorId, idempotencyKey);
            throw e;
        }
    }

    @Transactional
    public Optional<PostCreationRequest> tryReserve(UUID authorId, String key, String hash) {
        try {
            var reserved = self.reserve(authorId, key, hash);
            return Optional.of(reserved);
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PostCreationRequest reserve(UUID authorId, String key, String hash) {
        PostCreationRequest pcq = new PostCreationRequest(authorId, key, hash);
        return this.postCreationRequestRepository.saveAndFlush(pcq);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteReservation(UUID authorId, String idempotencyKey) {
        postCreationRequestRepository
                .findById_authorIdAndId_idempotencyKey(authorId, idempotencyKey)
                .ifPresent(postCreationRequestRepository::delete);
    }
}
