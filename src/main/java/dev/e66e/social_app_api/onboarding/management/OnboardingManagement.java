package dev.e66e.social_app_api.onboarding.management;

import dev.e66e.social_app_api.onboarding.*;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingData;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingRepository;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingStatus;
import dev.e66e.social_app_api.users.RegistrationData;
import dev.e66e.social_app_api.users.UserDTO;
import dev.e66e.social_app_api.users.UserRegistration;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class OnboardingManagement
        implements OnboardingExternalAPI, OnboardingInternalAPI {

    private final UserOnboardingRepository userOnboardingRepository;
    private final UserOnboardingEvaluator userOnboardingEvaluator;
    private final UserRegistration userRegistration;
    @Lazy private final OnboardingManagement self;

    @Override
    @Transactional
    public boolean isUserOnboarded(@Nullable UUID id) {
        if (id == null)
            throw new IllegalArgumentException("User id cannot be null.");

        Optional<UserOnboardingData> user = this.userOnboardingRepository.findById(id);

        if (user.isEmpty()) {
            self.addUserOnboarding(id);
            return false;
        }

        return user.get().getOnboardingStatus() == UserOnboardingStatus.COMPLETED;
    }

    @Transactional
    public UserOnboardingData addUserOnboarding(@Nullable UUID id) {
        if (id == null)
            throw new IllegalArgumentException("User id cannot be null.");

        UserOnboardingData newUserOnboarding = new UserOnboardingData();
        newUserOnboarding.setId(id);
        newUserOnboarding.setOnboardingStatus(UserOnboardingStatus.PENDING);
        return this.userOnboardingRepository.save(newUserOnboarding);
    }

    @Transactional
    public UserDTO userOnboarding(@Nullable UUID id,
                                  @Nullable UserOnboardingDataDTO userOnboardingDataDTO) {
        if (id == null)
            throw new IllegalArgumentException("User id cannot be null.");

        if (userOnboardingDataDTO == null)
            throw new IllegalArgumentException("User onboarding data cannot be null.");

        if (self.isUserOnboarded(id))
            throw new UserAlreadyOnboardedException("User already onboarded.");

        if (!this.userOnboardingEvaluator.check(userOnboardingDataDTO))
            throw new InvalidUserOnboardingData("Invalid user onboarding data.");

        UserOnboardingData userOnboardingData = this.userOnboardingRepository
                .findById(id).or(() -> Optional.of(self.addUserOnboarding(id))).get();

        UserDTO userDTO = this.userRegistration.register(new RegistrationData(
                    id,
                    Objects.requireNonNull(userOnboardingDataDTO.username()),
                    Objects.requireNonNull(userOnboardingDataDTO.publicUsername()),
                    Objects.requireNonNull(userOnboardingDataDTO.avatarUrl())
                )
        );
        userOnboardingData.setOnboardingStatus(UserOnboardingStatus.COMPLETED);

        return userDTO;
    }
}
