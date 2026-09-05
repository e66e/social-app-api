package dev.e66e.social_app_api.onboarding;

import java.util.UUID;

public interface OnboardingInternalAPI {

    boolean isUserOnboarded(UUID id);
}
