package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserDTO(
    UUID id,
    @NotBlank @Size(min = 5) String username,
    @NotBlank @Size(min = 5) String publicUsername,
    String avatarUrl,
    String bio,
    boolean isActive,
    UserRole role
) {
}
