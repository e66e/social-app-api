package dev.e66e.social_app_api.config;

import java.util.UUID;

public interface OnboardingStatusProvider {

    boolean isUserOnboarded(UUID id);
}
