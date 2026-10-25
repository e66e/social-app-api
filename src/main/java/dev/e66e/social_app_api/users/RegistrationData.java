package dev.e66e.social_app_api.users;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegistrationData(
        UUID id,
        @Size(min = 5, max = 30) @NotBlank
        String username,
        @Size(min = 5, max = 30) @NotBlank
        String publicUsername,
        @Max(100) @Nullable
        String avatarUrl
) {
}
