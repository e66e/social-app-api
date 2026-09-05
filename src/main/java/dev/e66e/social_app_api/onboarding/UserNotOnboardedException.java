package dev.e66e.social_app_api.onboarding;

public class UserNotOnboardedException extends RuntimeException {
    public UserNotOnboardedException(String message) {
        super(message);
    }
}
