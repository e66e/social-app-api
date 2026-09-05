package dev.e66e.social_app_api.gateway;

import java.time.Instant;

record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {
}
