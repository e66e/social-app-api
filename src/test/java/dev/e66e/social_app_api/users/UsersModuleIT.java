package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.config.OnboardingStatusProvider;
import dev.e66e.social_app_api.users.persistence.FollowRelationship;
import dev.e66e.social_app_api.users.persistence.FollowRelationshipRepository;
import dev.e66e.social_app_api.users.persistence.User;
import dev.e66e.social_app_api.users.persistence.UserRepository;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ModuleSlicing
@AutoConfigureMockMvc
@PostgresTestcontainers
class UsersModuleIT {

    @Autowired
    private MockMvc mockMvc;

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
                .avatarUrl("https://example.com/avatar.png")
                .bio("Hello, I am " + username)
                .isActive(true)
                .role(UserRole.USER)
                .isHotUser(false)
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Nested
    class UserControllerTests {

        @Test
        void shouldGetUserById() throws Exception {
            User user = createAndSaveUser("johndoe", "johnny");

            mockMvc.perform(get("/api/users/{identifier}", user.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.username").value("johndoe"))
                    .andExpect(jsonPath("$.publicUsername").value("johnny"))
                    .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                    .andExpect(jsonPath("$.bio").value("Hello, I am johndoe"));
        }

        @Test
        void shouldGetUserByPublicUsername() throws Exception {
            User user = createAndSaveUser("janedoe", "jane_public");

            mockMvc.perform(get("/api/users/{identifier}", "jane_public")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.username").value("janedoe"))
                    .andExpect(jsonPath("$.publicUsername").value("jane_public"));
        }

        @Test
        void shouldReturn404WhenUserNotFoundByUUID() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/users/{identifier}", nonExistentId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404WhenUserNotFoundByUsername() throws Exception {
            mockMvc.perform(get("/api/users/{identifier}", "non_existent_username")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UserManagementTests {

        @Test
        void shouldRegisterNewUser() {
            UUID id = UUID.randomUUID();
            RegistrationData regData = new RegistrationData(id, "newuser", "newbie", "https://img.com/avatar");

            UserRecord registered = userManagement.register(regData);

            assertThat(registered).isNotNull();
            assertThat(registered.id()).isEqualTo(id);
            assertThat(registered.username()).isEqualTo("newuser");
            assertThat(registered.publicUsername()).isEqualTo("newbie");

            Optional<UserRecord> fetched = userManagement.getUser(id);
            assertThat(fetched).isPresent();
            assertThat(fetched.get().username()).isEqualTo("newuser");
        }

        @Test
        void shouldCheckUsernameAvailability() {
            createAndSaveUser("takenuser", "takenpub");

            assertThat(userManagement.isUsernameAvailable("takenuser")).isFalse();
            assertThat(userManagement.isUsernameAvailable("availableuser")).isTrue();
        }

        @Test
        void shouldHandleFollowRelationships() {
            User userA = createAndSaveUser("usera", "user_a");
            User userB = createAndSaveUser("userb", "user_b");
            User userC = createAndSaveUser("userc", "user_c");

            // User B and User C follow User A
            followRelationshipRepository.saveAndFlush(new FollowRelationship(userB, userA));
            followRelationshipRepository.saveAndFlush(new FollowRelationship(userC, userA));

            // User A follows User B
            followRelationshipRepository.saveAndFlush(new FollowRelationship(userA, userB));

            Set<UUID> followersOfA = userManagement.getUsersFollowersIds(userA.getId());
            assertThat(followersOfA).containsExactlyInAnyOrder(userB.getId(), userC.getId());

            Set<UUID> followTargetsOfA = userManagement.getFollowTargetIds(userA.getId());
            assertThat(followTargetsOfA).containsExactly(userB.getId());

            Set<UserRecord> group = userManagement.getUserGroup(Set.of(userA.getId(), userB.getId()));
            assertThat(group).hasSize(2);
            assertThat(group).extracting(UserRecord::username).containsExactlyInAnyOrder("usera", "userb");
        }
    }
}
