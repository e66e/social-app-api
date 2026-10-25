package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.users.UserResponse;

import java.util.UUID;

public interface OnboardingExternalAPI {

    UserResponse userOnboarding(UUID userId, UserOnboardingDataDTO userOnboardingDataDTO);
}
