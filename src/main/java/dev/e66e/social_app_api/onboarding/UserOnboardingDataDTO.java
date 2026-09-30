package dev.e66e.social_app_api.onboarding;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserOnboardingDataDTO(
        @NotBlank @Size(min = 5) String username,
        @NotBlank @Size(min = 5) String publicUsername,
        @Nullable String avatarUrl
) {
    public UserOnboardingDataDTO {
        avatarUrl = avatarUrl == null ? "" : avatarUrl.trim();
    }
}
