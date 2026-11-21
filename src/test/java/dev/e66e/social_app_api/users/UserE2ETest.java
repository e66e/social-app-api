package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.config.OnboardingStatusProvider;
import dev.e66e.social_app_api.users.persistence.FollowRelationship;
import dev.e66e.social_app_api.users.persistence.FollowRelationshipRepository;
import dev.e66e.social_app_api.users.persistence.User;
import dev.e66e.social_app_api.users.persistence.UserRepository;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ModuleSlicing
@AutoConfigureMockMvc
@PostgresTestcontainers
@ActiveProfiles({"test"})
@DisplayName("User API End-to-End Tests")
class UserE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRelationshipRepository followRelationshipRepository;

    @Autowired
    private UserManagement userManagement;

    @MockitoBean
    private OnboardingStatusProvider onboardingStatusProvider;

    @BeforeEach
    void setUp() {
        followRelationshipRepository.deleteAll();
        userRepository.deleteAll();
        lenient().when(onboardingStatusProvider.isUserOnboarded(any())).thenReturn(true);
    }

    private User createAndSaveUser(String username, String publicUsername) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .publicUsername(publicUsername)
                .avatarUrl("https://example.com/" + username + ".png")
                .bio("Hello, I am " + username)
                .isActive(true)
                .role(UserRole.USER)
                .isHotUser(false)
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("GET /api/users/{username}")
    class GetUserEndpoint {

        @Test
        @DisplayName("returns 200 OK with UserResponse when queried by public username")
        void shouldGetUserByUsername() throws Exception {
            User user = createAndSaveUser("bob_jones", "bob_j");

            mockMvc.perform(get("/api/users/{identifier}", "bob_jones")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.username").value("bob_jones"))
                    .andExpect(jsonPath("$.publicUsername").value("bob_j"));
        }

        @Test
        @DisplayName("returns 404 NOT FOUND when user not found by UUID")
        void shouldReturn404WhenNotFoundByUUID() throws Exception {
            mockMvc.perform(get("/api/users/{identifier}", UUID.randomUUID())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 NOT FOUND when user not found by public username")
        void shouldReturn404WhenNotFoundByUsername() throws Exception {
            mockMvc.perform(get("/api/users/{identifier}", "unknown_user_public")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/users (Follow User)")
    class FollowUserEndpoint {

        private final String idempotencyKey = "k".repeat(32);

        @Test
        @DisplayName("returns 200 OK and establishes follow relationship for authenticated onboarded user")
        void shouldFollowUserSuccessfully() throws Exception {
            User follower = createAndSaveUser("follower_e2e", "follower_e2e_pub");
            User target = createAndSaveUser("target_e2e", "target_e2e_pub");

            FollowRelationshipRequest request = new FollowRelationshipRequest(target.getUsername(), idempotencyKey);

            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.followerId").value(follower.getId().toString()))
                    .andExpect(jsonPath("$.followTargetId").value(target.getId().toString()));

            // Verify relationship persisted in database
            Optional<FollowRelationship> saved = followRelationshipRepository.findById(follower.getId(), target.getId());
            assertThat(saved).isPresent();
        }

        @Test
        @DisplayName("returns 200 OK with same body when replaying request with identical idempotency key")
        void shouldBeIdempotentOnRepeatedRequests() throws Exception {
            User follower = createAndSaveUser("follower_idemp", "follower_idemp_pub");
            User target = createAndSaveUser("target_idemp", "target_idemp_pub");

            FollowRelationshipRequest request = new FollowRelationshipRequest(target.getUsername(), idempotencyKey);

            // First request
            String firstResponse = mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Replay request
            String secondResponse = mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(secondResponse).isEqualTo(firstResponse);
            assertThat(followRelationshipRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns 409 CONFLICT when same idempotency key is reused with different follow target")
        void shouldReturn409ConflictWhenReusingKeyWithDifferentTarget() throws Exception {
            User follower = createAndSaveUser("follower_conflict", "follower_conflict_pub");
            User target1 = createAndSaveUser("target1_conflict", "target1_conflict_pub");
            User target2 = createAndSaveUser("target2_conflict", "target2_conflict_pub");

            FollowRelationshipRequest request1 = new FollowRelationshipRequest(target1.getUsername(), idempotencyKey);
            FollowRelationshipRequest request2 = new FollowRelationshipRequest(target2.getUsername(), idempotencyKey);

            // First follow succeeds
            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk());

            // Second follow with conflicting payload returns 409
            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 404 NOT FOUND when follow target user does not exist")
        void shouldReturn404WhenTargetDoesNotExist() throws Exception {
            User follower = createAndSaveUser("follower_lonely", "follower_lonely_pub");
            FollowRelationshipRequest request = new FollowRelationshipRequest("nonexistingtarget", idempotencyKey);

            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 NOT FOUND when follower user does not exist in database")
        void shouldReturn404WhenFollowerDoesNotExist() throws Exception {
            User target = createAndSaveUser("target_alone", "target_alone_pub");
            UUID nonExistentFollowerId = UUID.randomUUID();
            FollowRelationshipRequest request = new FollowRelationshipRequest(target.getUsername(), idempotencyKey);

            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(nonExistentFollowerId.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 BAD REQUEST when request body violates validation constraints")
        void shouldReturn400WhenPayloadInvalid() throws Exception {
            User follower = createAndSaveUser("follower_bad", "follower_bad_pub");

            // Null targetId
            FollowRelationshipRequest nullTargetRequest = new FollowRelationshipRequest(null, idempotencyKey);
            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nullTargetRequest)))
                    .andExpect(status().isBadRequest());

            // Key too short (< 32 chars)
            FollowRelationshipRequest shortKeyRequest = new FollowRelationshipRequest("nonexistingusername", "too_short");
            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shortKeyRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 401 UNAUTHORIZED when no JWT token is provided")
        void shouldRejectUnauthenticatedRequest() throws Exception {
            FollowRelationshipRequest request = new FollowRelationshipRequest("randomUsername", idempotencyKey);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 403 FORBIDDEN when user is not onboarded according to UserOnboardingFilter")
        void shouldRejectWhenUserNotOnboarded() throws Exception {
            User follower = createAndSaveUser("follower_not_ob", "follower_not_ob_pub");
            User target = createAndSaveUser("target_for_ob", "target_for_ob_pub");

            when(onboardingStatusProvider.isUserOnboarded(follower.getId())).thenReturn(false);

            FollowRelationshipRequest request = new FollowRelationshipRequest(target.getUsername(), idempotencyKey);

            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(follower.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("User is not onboarded."));
        }
    }

    @Nested
    @DisplayName("End-to-End User Interaction Scenario")
    class CompleteUserScenario {

        @Test
        @DisplayName("full journey: user discovery, follow operation, follower query verification, and idempotent retry")
        void completeUserFollowLifecycle() throws Exception {
            // Given: Two users exist in system
            User alice = createAndSaveUser("alice_wonder", "alice_w");
            User bob = createAndSaveUser("bob_builder", "bob_b");
            String key = "f".repeat(40);

            // Step 1: Alice looks up Bob by his username
            mockMvc.perform(get("/api/users/{identifier}", "bob_builder")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(bob.getId().toString()))
                    .andExpect(jsonPath("$.username").value("bob_builder"));

            // Step 2: Alice follows Bob
            FollowRelationshipRequest followBob = new FollowRelationshipRequest(bob.getUsername(), key);
            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(alice.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followBob)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.followerId").value(alice.getId().toString()))
                    .andExpect(jsonPath("$.followTargetId").value(bob.getId().toString()));

            // Step 3: Verify system state via UserManagement queries
            Set<UUID> bobsFollowers = userManagement.getUsersFollowersIds(bob.getId());
            assertThat(bobsFollowers).containsExactly(alice.getId());

            Set<UUID> alicesFollowTargets = userManagement.getFollowTargetIds(alice.getId());
            assertThat(alicesFollowTargets).containsExactly(bob.getId());

            // Step 4: Alice replays the follow request (e.g. network retry), verifying idempotent success
            mockMvc.perform(post("/api/users")
                            .with(jwt().jwt(jwt -> jwt.subject(alice.getId().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(followBob)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.followerId").value(alice.getId().toString()))
                    .andExpect(jsonPath("$.followTargetId").value(bob.getId().toString()));

            // Verify only one relationship remains in the database
            assertThat(followRelationshipRepository.count()).isEqualTo(1);
        }
    }
}
