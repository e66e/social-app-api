package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.users.UserInternalAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StandardUserOnboardingEvaluator implements UserOnboardingEvaluator {

    private final UserInternalAPI userApi;

    @Override
    public boolean check(UserOnboardingDataDTO userOnboardingDataDTO) {
        String username = userOnboardingDataDTO.username().trim();
        String publicUsername = userOnboardingDataDTO.publicUsername().trim();

        if (username.length() < 5 || publicUsername.length() < 5)
            return false;

        return this.userApi.isUsernameAvailable(username);
    }
}
