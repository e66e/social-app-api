package dev.e66e.social_app_api.posts.idempotency;

import dev.e66e.social_app_api.posts.PostResponse;

import java.util.UUID;
import java.util.function.Supplier;

public interface PostIdempotencyAPI {

    UUID executeIdempotent(UUID authorId, String idempotencyKey, String requestHash, Supplier<PostResponse> callback);
}
