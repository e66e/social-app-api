package dev.e66e.social_app_api.users.persistence;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsById(@NonNull UUID id);
    boolean existsByUsername(@NonNull String username);
}
