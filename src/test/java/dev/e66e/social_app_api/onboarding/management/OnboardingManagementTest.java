package dev.e66e.social_app_api.onboarding.management;

import dev.e66e.social_app_api.onboarding.InvalidUserOnboardingData;
import dev.e66e.social_app_api.onboarding.UserAlreadyOnboardedException;
import dev.e66e.social_app_api.onboarding.UserOnboardingDataDTO;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingData;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingRepository;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingStatus;
import dev.e66e.social_app_api.users.RegistrationData;
import dev.e66e.social_app_api.users.UserDTO;
import dev.e66e.social_app_api.users.UserRegistration;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingManagementTest {

    @Mock
    private UserOnboardingRepository userOnboardingRepository;
    @Mock
    private UserOnboardingEvaluator userOnboardingEvaluator;
    @Mock
    private UserRegistration userRegistration;

    @InjectMocks
    private OnboardingManagement onboardingManagement;

    @Nested
    @DisplayName("isUserOnboarded method tests.")
    class IsUserOnboardedTests {

        @Test
        @DisplayName("User already onboarded test.")
        void ifUserAlreadyOnboarded_thenReturnTrue() {
            // Given
            UUID id = UUID.randomUUID();
            when(userOnboardingRepository.findById(id))
                    .thenReturn(Optional.of(new UserOnboardingData(id, UserOnboardingStatus.COMPLETED)));

            // When
            boolean result = onboardingManagement.isUserOnboarded(id);

            // Then
            assertTrue(result);
            verify(userOnboardingRepository, atMostOnce()).findById(any());
        }

        @Test
        @DisplayName("User in database, but not onboarded test.")
        void ifUserInDatabaseAndNotOnboarded_thenReturnFalse() {
            // Given
            UUID id = UUID.randomUUID();
            when(userOnboardingRepository.findById(id))
                    .thenReturn(Optional.of(new UserOnboardingData(id, UserOnboardingStatus.PENDING)));

            // When
            boolean result = onboardingManagement.isUserOnboarded(id);

            // Then
            assertFalse(result);
            verify(userOnboardingRepository, atMostOnce()).findById(any());
        }

        @Test
        @DisplayName("User not in database, then isn't onboarded.")
        void ifUserNotInDatabase_thenReturnFalse() {
            // Given
            UUID id = UUID.randomUUID();
            when(userOnboardingRepository.findById(id))
                    .thenReturn(Optional.empty());

            // When
            boolean result = onboardingManagement.isUserOnboarded(id);

            // Then
            assertFalse(result);
            verify(userOnboardingRepository, atMostOnce()).findById(any());
        }

        @Test
        @DisplayName("Null user id value as method param.")
        void idPassedIsNull_thenThrowsIllegalArgumentException() {
            // Given

            // When/Then
            assertThrows(IllegalArgumentException.class,
                    () -> onboardingManagement.isUserOnboarded(null));

            verify(userOnboardingRepository, never()).findById(any());
        }
    }


    @Nested
    @DisplayName("addUserOnboarding tests.")
    class AddUserOnboarding {

        @Test
        @DisplayName("Happy path test")
        void happyPath() {
            // Given
            UUID id = UUID.randomUUID();

            // When / Then
            assertDoesNotThrow(() -> onboardingManagement.addUserOnboarding(id));
            verify(userOnboardingRepository, atMostOnce()).findById(any());
        }

        @Test
        @DisplayName("Null passed as an id test.")
        void givenNullId_shouldThrowIllegalArgumentException() {
            // Given

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> onboardingManagement.addUserOnboarding(null));
            verify(userOnboardingRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("userOnboarding method tests.")
    class UserOnboarding {

        @Test
        @DisplayName("Happy path.")
        void happyPath() {
            // Given
            UUID id = UUID.randomUUID();
            UserOnboardingDataDTO dto
                    = new UserOnboardingDataDTO(
                            "correctUsername",
                    "correctPublicUsername",
                    "");
            UserDTO userDTO = new UserDTO(
                    id,
                    dto.username(),
                    dto.publicUsername(),
                    "",
                    "",
                    true,
                    UserRole.USER
            );
            UserOnboardingData foundById = new UserOnboardingData(
                    id, UserOnboardingStatus.PENDING);
            RegistrationData registrationData = new RegistrationData(
                    id,
                    Objects.requireNonNull(dto.username()),
                    Objects.requireNonNull(dto.publicUsername()),
                    Objects.requireNonNull(dto.avatarUrl())
            );

            when(userOnboardingEvaluator.check(dto)).thenReturn(true);
            when(userOnboardingRepository.findById(id))
                    .thenReturn(Optional.of(foundById));
            when(userRegistration.register(registrationData))
                    .thenReturn(userDTO);
            // When
            UserDTO returnValue = onboardingManagement.userOnboarding(id, dto);

            // Then
            assertEquals(userDTO, returnValue);
            verify(userOnboardingEvaluator, times(1)).check(dto);
            verify(userOnboardingRepository, times(2)).findById(any());
            verify(userRegistration, times(1)).register(any());
        }

        @Test
        @DisplayName("Null passed as id argument.")
        void passedNullId_shouldThrowIllegalArgumentException() {
            // Given

            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> onboardingManagement.userOnboarding(null,
                            null),
                    "User id cannot be null."
            );

            verify(userOnboardingEvaluator, never()).check(any());
            verify(userOnboardingRepository, never()).findById(any());
            verify(userRegistration, never()).register(any());
        }

        @Test
        @DisplayName("Null passed as userOnboardingDataDTO.")
        void passedNullDto_shouldThrowIllegalArgumentException() {
            // Given

            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> onboardingManagement.userOnboarding(UUID.randomUUID(),
                            null),
                    "User id cannot be null."
            );

            verify(userOnboardingEvaluator, never()).check(any());
            verify(userOnboardingRepository, never()).findById(any());
            verify(userRegistration, never()).register(any());
        }

        @Test
        @DisplayName("When user onboarded method should throw UserAlreadyOnboardedException.")
        void userAlreadyOnboarded_shouldThrowUserAlreadyOnboardedException() {
            // Given
            UUID id = UUID.randomUUID();
            UserOnboardingDataDTO dto = new UserOnboardingDataDTO(
                    "username",
                    "publicUsername",
                    ""
            );

            when(userOnboardingRepository.findById(id))
                    .thenReturn(Optional.of(
                            new UserOnboardingData(id,
                                    UserOnboardingStatus.COMPLETED)
                    ));



            // When / Then
            assertThrows(UserAlreadyOnboardedException.class,
                    () -> onboardingManagement.userOnboarding(id, dto));
        }

        @Test
        @DisplayName("Passed userOnboardingDataDTO didn't pass check.")
        void passedDtoDidntPassCheck_shouldThrowInvalidUserOnboardingData() {
            // Given
            UUID id = UUID.randomUUID();
            UserOnboardingDataDTO dto = new UserOnboardingDataDTO(
                    "usr",
                    "pusr",
                    ""
            );
            when(userOnboardingEvaluator.check(dto)).thenReturn(false);

            // When / Then
            assertThrows(InvalidUserOnboardingData.class,
                    () -> onboardingManagement.userOnboarding(id, dto));
        }
        @Test
        @DisplayName("""
            UserOnboardingDTO passed check, but id wasn't in database.
            Should call addUserOnboarding method and register user.""")
        void passedIdDidntExistedInDb_shouldAddUserOnboarding() {
            // Given
            UUID id = UUID.randomUUID();
            UserOnboardingDataDTO dto
                    = new UserOnboardingDataDTO(
                    "correctUsername",
                    "correctPublicUsername",
                    "");
            UserDTO userDTO = new UserDTO(
                    id,
                    dto.username(),
                    dto.publicUsername(),
                    "",
                    "",
                    true,
                    UserRole.USER
            );
            RegistrationData registrationData = new RegistrationData(
                    id,
                    Objects.requireNonNull(dto.username()),
                    Objects.requireNonNull(dto.publicUsername()),
                    Objects.requireNonNull(dto.avatarUrl())
            );

            when(userOnboardingEvaluator.check(dto)).thenReturn(true);
            when(userOnboardingRepository.findById(id))
                    .thenReturn(Optional.empty());
            when(userRegistration.register(registrationData))
                    .thenReturn(userDTO);
            UserOnboardingData newUserOnboarding = new UserOnboardingData(id, UserOnboardingStatus.PENDING);
            when(userOnboardingRepository.saveAndFlush(newUserOnboarding))
                    .thenReturn(newUserOnboarding);

            // When
            UserDTO returnValue = onboardingManagement.userOnboarding(id, dto);

            // Then
            assertEquals(userDTO, returnValue);
            verify(userOnboardingEvaluator).check(dto);
            verify(userOnboardingRepository, times(2)).findById(any());
            verify(userRegistration).register(any());
        }
    }
}