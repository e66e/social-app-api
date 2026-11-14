package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.users.UserInternalAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class StandardUserOnboardingEvaluator implements UserOnboardingEvaluator {

    private final UserInternalAPI userApi;

    @Override
    public boolean check(UserOnboardingDataDTO userOnboardingDataDTO) {
        log.debug("Checking user onboarding data: {}.", userOnboardingDataDTO);
        String username = userOnboardingDataDTO.username().trim();
        String publicUsername = userOnboardingDataDTO.publicUsername().trim();

        if (username.length() < 5 || publicUsername.length() < 5) {
            log.debug("Username or public username is too short: {}, {}.", username, publicUsername);
            return false;
        }

        return this.userApi.isUsernameAvailable(username);
    }
}
