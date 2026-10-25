package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.users.UserResponse;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
class OnboardingController {

    private final OnboardingExternalAPI onboarding;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse onboarding(@Valid @RequestBody UserOnboardingDataDTO userOnboardingDataDTO,
                                   @Nullable Authentication auth) {
        if (auth == null)
            throw new InvalidBearerTokenException("Bearer token required.");

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt token = jwtAuth.getToken();
            String subjectStr = Objects.requireNonNull(token.getSubject());
            UUID id = UUID.fromString(subjectStr);
            return this.onboarding.userOnboarding(id, userOnboardingDataDTO);
        }
        throw new InvalidBearerTokenException("Invalid token type.");
    }

    @ExceptionHandler(InvalidUserOnboardingData.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Map<String, Object> invalidUserOnboardingDataExceptionHandler(Exception e) {
        return Map.of(
                "statusCode", HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "message", e.getMessage()
        );
    }

    @ExceptionHandler(UserAlreadyOnboardedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> userAlreadyOnboardedExceptionHandler(Exception e) {
        return Map.of(
                "statusCode", HttpStatus.CONFLICT.value(),
                "message", e.getMessage()
        );
    }
}
