package dev.e66e.social_app_api.onboarding.management;

import dev.e66e.social_app_api.onboarding.UserOnboardingDataDTO;
import dev.e66e.social_app_api.users.UserInternalAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StandardUserOnboardingEvaluator implements UserOnboardingEvaluator {

    private final UserInternalAPI userApi;

    @Override
    public boolean check(UserOnboardingDataDTO userOnboardingDataDTO) {
        String username = userOnboardingDataDTO.username().trim();
        String publicUsername = userOnboardingDataDTO.publicUsername().trim();
        String avatarUrl = userOnboardingDataDTO.avatarUrl();
        avatarUrl = avatarUrl == null ? "" : avatarUrl.trim();

        if (username.length() < 5 || publicUsername.length() < 5)
            return false;

        return this.userApi.isUsernameAvailable(username);
    }
}
