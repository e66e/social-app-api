package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.users.UserRecord;

import java.util.UUID;

public interface OnboardingExternalAPI {

    UserRecord userOnboarding(UUID id, UserOnboardingDataDTO userOnboardingDataDTO);
}
