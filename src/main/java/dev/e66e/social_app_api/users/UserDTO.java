package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.UserRole;

import java.util.UUID;

public record UserDTO(
    UUID id,
    String email,
    String username,
    String publicUsername,
    String avatarUrl,
    String bio,
    boolean isActive,
    UserRole role
) {
}
