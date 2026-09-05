package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.users.UserDTO;

import java.util.UUID;

public interface OnboardingExternalAPI {

    UserDTO userOnboarding(UUID id, UserOnboardingDataDTO userOnboardingDataDTO);
}
