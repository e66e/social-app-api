package dev.e66e.social_app_api.onboarding;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingData;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingRepository;
import dev.e66e.social_app_api.onboarding.persistence.UserOnboardingStatus;
import dev.e66e.social_app_api.users.*;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ModuleSlicing
@AutoConfigureMockMvc
@PostgresTestcontainers
class OnboardingModuleIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserOnboardingRepository userOnboardingRepository;

    @MockitoBean
    private UserRegistration userRegistration;

    @MockitoBean
    private UserInternalAPI userApi;

    @MockitoBean
    private UserExternalAPI userExternalAPI;

    @BeforeEach
    void setUp() {
        userOnboardingRepository.deleteAll();
    }

    @Test
    void shouldCompleteOnboardingSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UserOnboardingDataDTO dto = new UserOnboardingDataDTO("validuser",
                "valid_pub",
                "https://avatar.url");

        when(userApi.isUsernameAvailable("validuser")).thenReturn(true);
        when(userRegistration.register(any(RegistrationData.class)))
                .thenAnswer(invocation -> {
            RegistrationData reg = invocation.getArgument(0);
            return new UserRecord(
                    reg.id(),
                    reg.username(),
                    reg.publicUsername(),
                    reg.avatarUrl(),
                    "",
                    true,
                    UserRole.USER,
                    false
            );
        });
        UserResponse userResponse = new UserResponse(userId,
                "validuser",
                "valid_pub",
                "https://avatar.url",
                "");
        when(userExternalAPI.getUserById(eq(userId)))
                .thenReturn(Optional.of(userResponse));

        mockMvc.perform(post("/api/onboarding")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("validuser"))
                .andExpect(jsonPath("$.publicUsername").value("valid_pub"));

        Optional<UserOnboardingData> record = userOnboardingRepository.findById(userId);
        assertThat(record).isPresent();
        assertThat(record.get().getOnboardingStatus()).isEqualTo(UserOnboardingStatus.COMPLETED);
    }

    @Test
    void shouldReturnExistingUserWhenAlreadyOnboarded() throws Exception {
        UUID userId = UUID.randomUUID();
        UserOnboardingData data = new UserOnboardingData();
        data.setId(userId);
        data.setOnboardingStatus(UserOnboardingStatus.COMPLETED);
        userOnboardingRepository.saveAndFlush(data);
        UserResponse userResponse = new UserResponse(userId,
                "secondattempt",
                "second_pub",
                "",
                "");
        when(userExternalAPI.getUserById(userId)).thenReturn(Optional.of(userResponse));

        UserOnboardingDataDTO dto = new UserOnboardingDataDTO("secondattempt",
                "second_pub",
                null);

        mockMvc.perform(post("/api/onboarding")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBadRequestWhenUsernameTooShort() throws Exception {
        UUID userId = UUID.randomUUID();
        // username length < 5 violates @Size(min = 5)
        UserOnboardingDataDTO dto = new UserOnboardingDataDTO("shrt", "short_pub", null);

        mockMvc.perform(post("/api/onboarding")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntityWhenUsernameNotAvailable() throws Exception {
        UUID userId = UUID.randomUUID();
        UserOnboardingDataDTO dto = new UserOnboardingDataDTO("takenuser", "taken_pub", null);

        when(userApi.isUsernameAvailable("takenuser")).thenReturn(false);

        mockMvc.perform(post("/api/onboarding")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.statusCode").value(422));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        UserOnboardingDataDTO dto = new UserOnboardingDataDTO("someuser", "some_pub", null);

        mockMvc.perform(post("/api/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
