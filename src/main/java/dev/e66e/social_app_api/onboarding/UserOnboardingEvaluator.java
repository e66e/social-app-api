package dev.e66e.social_app_api.onboarding;

@FunctionalInterface
public interface UserOnboardingEvaluator {

    boolean check(UserOnboardingDataDTO userOnboardingDataDTO);
}
