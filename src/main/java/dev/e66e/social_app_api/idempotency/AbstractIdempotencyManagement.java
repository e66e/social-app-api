package dev.e66e.social_app_api.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractIdempotencyManagement<E extends AbstractIdempotencyRecord>
        implements IdempotencyInternalAPI {

    private final IdempotencyRecordRepository<E> repository;
    private final TransactionTemplate requiresNewTx;
    private final String resourceName;

    protected AbstractIdempotencyManagement(
            IdempotencyRecordRepository<E> repository,
            PlatformTransactionManager txManager,
            String resourceName
    ) {
        this.repository = repository;
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.resourceName = resourceName;
    }

    protected abstract E newRecord(UUID authorId, String idempotencyKey, String requestHash);

    @Override
    public UUID executeIdempotent(UUID authorId,
                                  String idempotencyKey,
                                  String requestHash,
                                  Supplier<UUID> callback) {

        Optional<E> reservedOpt = this.tryReserve(authorId, idempotencyKey, requestHash);

        if (reservedOpt.isEmpty()) {
            E record = this.repository.findByIdempotencyKey(authorId, idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Request exists but was not found.")
                    );

            return switch (record.getStatus()) {
                case IN_PROGRESS -> throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Other thread handles request with given body and idempotency key.");

                case COMPLETED -> {
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Idempotency key already used for " + resourceName
                                        + " with id: " + record.getResourceId());
                    }
                    yield record.getResourceId();
                }
            };
        }

        E record = reservedOpt.get();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        deleteReservation(authorId, idempotencyKey);
                    }
                }
            });
        }

        try {
            UUID resourceId = callback.get();
            record.markCompleted(resourceId);
            repository.saveAndFlush(record);
            return record.getResourceId();
        } catch (RuntimeException e) {
            deleteReservation(authorId, idempotencyKey);
            throw e;
        }
    }

    protected Optional<E> tryReserve(UUID authorId, String key, String hash) {
        try {
            E record = this.requiresNewTx.execute(_ -> {
                E fresh = newRecord(authorId, key, hash);
                return this.repository.saveAndFlush(fresh);
            });
            return Optional.of(Objects.requireNonNull(record));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    void deleteReservation(UUID authorId, String key) {
        this.requiresNewTx.executeWithoutResult(_ ->
            this.repository.findByIdempotencyKey(authorId, key)
                    .ifPresent(this.repository::delete));
    }
}
