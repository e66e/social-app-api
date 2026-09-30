package dev.e66e.social_app_api.onboarding;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserOnboardingDataDTO tests.")
class UserOnboardingDataDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Correct input data for DTO test.")
    void userOnboarding_correctData_noViolations() {
        UserOnboardingDataDTO dataDTO = new UserOnboardingDataDTO(
                "username",
                "correctPublicUsername",
                null
        );

        Set<ConstraintViolation<UserOnboardingDataDTO>> violations = validator.validate(dataDTO);

        assertEquals(0, violations.size());
    }

    @Test
    @DisplayName("Null username passed to DTO.")
    void userOnboarding_nullUsernameValue_thenShouldHaveConstraintViolation() {
        @SuppressWarnings("DataFlowIssue")
        UserOnboardingDataDTO dataDTO = new UserOnboardingDataDTO(
                null,
                "correctPublicUsername",
                null
        );

        Set<ConstraintViolation<UserOnboardingDataDTO>> violations = validator.validate(dataDTO);

        assertEquals(1, violations.size());

        ConstraintViolation<UserOnboardingDataDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("username");
    }

    @Test
    @DisplayName("Too short username passed to DTO.")
    void userOnboarding_tooShortUsernameValue_thenShouldHaveConstraintViolation() {
        UserOnboardingDataDTO dataDTO = new UserOnboardingDataDTO(
                "usr",
                "correctPublicUsername",
                null
        );

        Set<ConstraintViolation<UserOnboardingDataDTO>> violations = validator.validate(dataDTO);

        assertEquals(1, violations.size());

        ConstraintViolation<UserOnboardingDataDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("username");
    }

    @Test
    @DisplayName("Null passed as public username field.")
    void userOnboarding_nullPublicUsernameValue_thenShouldHaveConstraintViolation() {
        @SuppressWarnings("DataFlowIssue")
        UserOnboardingDataDTO dataDTO = new UserOnboardingDataDTO(
                "correctUsername",
                null,
                null
        );

        Set<ConstraintViolation<UserOnboardingDataDTO>> violations = validator.validate(dataDTO);

        assertEquals(1, violations.size());

        ConstraintViolation<UserOnboardingDataDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("publicUsername");
    }

    @Test
    @DisplayName("Too short value passed as public username field.")
    void userOnboarding_tooShortPublicUsernameValue_thenShouldHaveConstraintViolation() {
        UserOnboardingDataDTO dataDTO = new UserOnboardingDataDTO(
                "correctUsername",
                "pubU",
                null
        );

        Set<ConstraintViolation<UserOnboardingDataDTO>> violations = validator.validate(dataDTO);

        assertEquals(1, violations.size());

        ConstraintViolation<UserOnboardingDataDTO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("publicUsername");
    }

}