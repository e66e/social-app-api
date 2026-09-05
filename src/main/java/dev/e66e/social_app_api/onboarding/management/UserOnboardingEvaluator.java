package dev.e66e.social_app_api.onboarding.management;

import dev.e66e.social_app_api.onboarding.UserOnboardingDataDTO;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingData;

@FunctionalInterface
public interface UserOnboardingEvaluator {

    boolean check(UserOnboardingDataDTO userOnboardingDataDTO);
}
