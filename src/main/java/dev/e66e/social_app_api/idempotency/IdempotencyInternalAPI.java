package dev.e66e.social_app_api.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

public interface IdempotencyInternalAPI {

    UUID executeIdempotent(UUID authorId, String idempotencyKey, String requestHash, Supplier<UUID> callback);
}
