package dev.e66e.social_app_api.users;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserResponse(
        UUID id,
        @NotBlank @Size(min = 5) String username,
        @NotBlank @Size(min = 5) String publicUsername,
        @Nullable String avatarUrl,
        @Nullable String bio
) {}
