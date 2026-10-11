package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.UserRole;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserRecord(
    UUID id,
    @NotBlank @Size(min = 5) String username,
    @NotBlank @Size(min = 5) String publicUsername,
    @Nullable String avatarUrl,
    @Nullable String bio,
    boolean isActive,
    UserRole role,
    boolean isHotUser
) {
    public UserRecord {
        avatarUrl = avatarUrl == null ? "" : avatarUrl;
        bio = bio == null ? "" : bio;
    }
}
