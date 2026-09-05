package dev.e66e.social_app_api.onboarding;

public class InvalidUserOnboardingData extends RuntimeException {
    public InvalidUserOnboardingData(String message) {
        super(message);
    }
}
