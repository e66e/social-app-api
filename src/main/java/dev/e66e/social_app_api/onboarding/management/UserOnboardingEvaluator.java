package dev.e66e.social_app_api.onboarding.management;

import dev.e66e.social_app_api.onboarding.UserOnboardingDataDTO;

@FunctionalInterface
public interface UserOnboardingEvaluator {

    boolean check(UserOnboardingDataDTO userOnboardingDataDTO);
}
