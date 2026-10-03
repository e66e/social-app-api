package dev.e66e.social_app_api.posts;

import java.util.UUID;

public record PostPublishedEvent(UUID createdPostId) {}
