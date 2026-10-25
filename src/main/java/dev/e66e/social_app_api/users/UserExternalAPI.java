package dev.e66e.social_app_api.users;

import java.util.Optional;
import java.util.UUID;

public interface UserExternalAPI {

    Optional<UserResponse> getUserById(UUID userId);

    Optional<UserResponse> getUserByPublicUsername(String username);
}
