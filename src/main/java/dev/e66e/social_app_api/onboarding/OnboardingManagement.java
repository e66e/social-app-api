package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingData;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingRepository;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingStatus;
import dev.e66e.social_app_api.users.*;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class OnboardingManagement
        implements OnboardingExternalAPI, OnboardingInternalAPI { // TODO remake onboarding

    private final UserOnboardingRepository userOnboardingRepository;
    private final UserOnboardingEvaluator userOnboardingEvaluator;
    private final UserRegistration userRegistration;
    private final UserExternalAPI userExternalAPI;

    @Override
    @Transactional
    public boolean isUserOnboarded(@Nullable UUID id) {
        if (id == null)
            throw new IllegalArgumentException("User id cannot be null.");

        Optional<UserOnboardingData> user = this.userOnboardingRepository.findById(id);

        if (user.isEmpty()) {
            log.debug("No onboarding record found with id={}. Onboarding record will be added.", id);
            this.addUserOnboarding(id);
            return false;
        }

        return user.get().getOnboardingStatus() == UserOnboardingStatus.COMPLETED;
    }

    UserOnboardingData addUserOnboarding(@Nullable UUID id) {
        if (id == null)
            throw new IllegalArgumentException("User id cannot be null.");

        log.debug("Adding onboarding record with id={}.", id);
        UserOnboardingData newUserOnboarding = new UserOnboardingData();
        newUserOnboarding.setId(id);
        newUserOnboarding.setOnboardingStatus(UserOnboardingStatus.PENDING);
        return this.userOnboardingRepository.saveAndFlush(newUserOnboarding);
    }

    @Override
    @Transactional
    public UserResponse userOnboarding(@Nullable UUID id,
                                       @Nullable UserOnboardingDataDTO userOnboardingDataDTO) {
        if (id == null)
            throw new IllegalArgumentException("User id cannot be null.");

        if (userOnboardingDataDTO == null)
            throw new IllegalArgumentException("User onboarding data cannot be null.");

        if (this.isUserOnboarded(id)) {
            return this.userExternalAPI.getUserById(id).orElseThrow(() -> {
                log.error("User with id={} was diagnosed as onboarded, but was not found in users db.", id);
                return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "User was declared as onboarded but was not found in db.");
            });
        }

        if (!this.userOnboardingEvaluator.check(userOnboardingDataDTO))
            throw new InvalidUserOnboardingData("Invalid user onboarding data.");

        UserOnboardingData userOnboardingData = this.userOnboardingRepository
                .findById(id).or(() -> Optional.of(this.addUserOnboarding(id))).get();

        UserRecord userRecord = this.userRegistration.register(new RegistrationData(
                    id,
                    Objects.requireNonNull(userOnboardingDataDTO.username()),
                    Objects.requireNonNull(userOnboardingDataDTO.publicUsername()),
                    Objects.requireNonNull(userOnboardingDataDTO.avatarUrl())
                )
        );
        userOnboardingData.setOnboardingStatus(UserOnboardingStatus.COMPLETED);

        return this.userExternalAPI.getUserById(userRecord.id())
                .orElseThrow(() -> {
                    log.error("User with id={} was successfully onboarded, but later on was not found in users db.",
                            id);
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error while fetching user data.");
                });
    }
}
