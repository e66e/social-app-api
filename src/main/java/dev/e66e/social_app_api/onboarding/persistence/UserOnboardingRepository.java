package dev.e66e.social_app_api.onboarding.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOnboardingRepository extends JpaRepository<UserOnboardingData, UUID> {
}
