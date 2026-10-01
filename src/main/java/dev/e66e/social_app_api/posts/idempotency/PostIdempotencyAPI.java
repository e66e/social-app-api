package dev.e66e.social_app_api.posts.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

public interface PostIdempotencyAPI {

    UUID executeIdempotent(UUID authorId, String idempotencyKey, String requestHash, Supplier<UUID> callback);
}
