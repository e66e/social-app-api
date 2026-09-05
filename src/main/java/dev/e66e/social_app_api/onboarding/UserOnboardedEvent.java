package dev.e66e.social_app_api.onboarding;

import java.util.UUID;

public record UserOnboardedEvent(
        UUID id,
        String username,
        String publicUsername,
        String avatarUrl
) {
}
