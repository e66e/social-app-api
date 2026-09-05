package dev.e66e.social_app_api.users;

import jakarta.annotation.Nullable;

import java.util.UUID;

public record RegistrationData(
        UUID id,
        String username,
        String publicUsername,
        @Nullable String avatarUrl
) {
}
