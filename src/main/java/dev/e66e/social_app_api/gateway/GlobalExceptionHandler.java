package dev.e66e.social_app_api.gateway;

import dev.e66e.social_app_api.onboarding.InvalidUserOnboardingData;
import dev.e66e.social_app_api.onboarding.UserNotOnboardedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidUserOnboardingData.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErrorResponse invalidPayloadData(InvalidUserOnboardingData ex) {
        return new ErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(),
                ex.getMessage(),
                Instant.now()
        );
    }

    @ExceptionHandler(UserNotOnboardedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorResponse userNotOnboarded(UserNotOnboardedException ex) {
        return new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                Instant.now()
        );
    }
}
