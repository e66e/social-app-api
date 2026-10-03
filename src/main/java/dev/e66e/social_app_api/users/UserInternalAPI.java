package dev.e66e.social_app_api.users;

import java.util.Optional;
import java.util.UUID;

public interface UserInternalAPI {

    boolean isUsernameAvailable(String username);

    Optional<UserRecord> getUser(UUID authorId);
}
