package dev.e66e.social_app_api.onboarding;

public class UserAlreadyOnboardedException extends RuntimeException {
    public UserAlreadyOnboardedException(String message) {
        super(message);
    }
}
