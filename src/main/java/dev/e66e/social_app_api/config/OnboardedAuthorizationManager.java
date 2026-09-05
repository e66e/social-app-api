package dev.e66e.social_app_api.config;

import dev.e66e.social_app_api.onboarding.OnboardingInternalAPI;
import dev.e66e.social_app_api.onboarding.UserNotOnboardedException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
class OnboardedAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final OnboardingInternalAPI onboarding;

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
                                                   RequestAuthorizationContext object) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidBearerTokenException("Authentication required");
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String subject = jwt.getSubject();
            if (subject == null)
                throw new InvalidBearerTokenException("Subject required");

            try {
                UUID kcUserId = UUID.fromString(subject);
                boolean isOnboarded = onboarding.isUserOnboarded(kcUserId);
                if (!isOnboarded)
                    throw new UserNotOnboardedException("User not onboarded.");
                return () -> true;
            } catch (IllegalArgumentException e) {
                throw new InvalidBearerTokenException("Invalid token's id format.");
            }
        }
        throw new InvalidBearerTokenException("Invalid token type.");
    }
}
