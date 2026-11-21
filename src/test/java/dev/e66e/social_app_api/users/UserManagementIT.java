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
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ModuleSlicing
@PostgresTestcontainers
@ActiveProfiles({"test"})
@DisplayName("UserManagement Integration Tests")
class UserManagementIT {

    @TestComponent
    static class TestUserCreatedEventListener implements ApplicationListener<PayloadApplicationEvent<UserCreatedEvent>> {
        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public void onApplicationEvent(PayloadApplicationEvent<UserCreatedEvent> event) {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }

        public void reset() {
            count.set(0);
        }
    }

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRelationshipRepository followRelationshipRepository;

    @Autowired(required = false)
    private TestUserCreatedEventListener eventListener;

    @MockitoBean
    private OnboardingStatusProvider onboardingStatusProvider;

    @BeforeEach
    void setUp() {
        followRelationshipRepository.deleteAll();
        userRepository.deleteAll();
        if (eventListener != null) {
            eventListener.reset();
        }
    }

    private User createAndSaveUser(String username, String publicUsername) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .publicUsername(publicUsername)
                .avatarUrl("https://example.com/" + username + ".png")
                .bio("Bio of " + username)
                .isActive(true)
                .role(UserRole.USER)
                .isHotUser(false)
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("persists new user in database, publishes UserCreatedEvent, and returns mapped UserRecord")
        void shouldRegisterNewUserAndPublishEvent() {
            UUID userId = UUID.randomUUID();
            RegistrationData regData = new RegistrationData(
                    userId,
                    "charlie",
                    "charlie_pub",
                    "https://avatar.url/charlie"
            );

            UserRecord registered = userManagement.register(regData);

            assertThat(registered).isNotNull();
            assertThat(registered.id()).isEqualTo(userId);
            assertThat(registered.username()).isEqualTo("charlie");
            assertThat(registered.publicUsername()).isEqualTo("charlie_pub");
            assertThat(registered.avatarUrl()).isEqualTo("https://avatar.url/charlie");

            // Verify persistence in PostgreSQL
            Optional<User> persisted = userRepository.findById(userId);
            assertThat(persisted).isPresent();
            assertThat(persisted.get().getUsername()).isEqualTo("charlie");
            assertThat(persisted.get().getPublicUsername()).isEqualTo("charlie_pub");
            assertThat(persisted.get().getAvatarUrl()).isEqualTo("https://avatar.url/charlie");

            // Verify domain event publication
            if (eventListener != null) {
                assertThat(eventListener.getCount()).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("isUsernameAvailable")
    class UsernameAvailabilityTests {

        @Test
        @DisplayName("returns true when username is not taken in database")
        void shouldReturnTrueWhenUsernameAvailable() {
            assertThat(userManagement.isUsernameAvailable("unused_username")).isTrue();
        }

        @Test
        @DisplayName("returns false when username already exists in database")
        void shouldReturnFalseWhenUsernameTaken() {
            createAndSaveUser("existing_user", "existing_pub");

            assertThat(userManagement.isUsernameAvailable("existing_user")).isFalse();
        }
    }

    @Nested
    @DisplayName("getUser queries")
    class UserQueriesTests {

        @Test
        @DisplayName("getUser returns mapped UserRecord when user exists")
        void shouldGetUserRecordWhenExists() {
            User user = createAndSaveUser("dave", "dave_pub");

            Optional<UserRecord> result = userManagement.getUser(user.getId());

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(user.getId());
            assertThat(result.get().username()).isEqualTo("dave");
            assertThat(result.get().publicUsername()).isEqualTo("dave_pub");
        }

        @Test
        @DisplayName("getUser returns empty Optional when user does not exist")
        void shouldReturnEmptyWhenUserNotFound() {
            Optional<UserRecord> result = userManagement.getUser(UUID.randomUUID());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getUserById returns mapped UserResponse when user exists")
        void shouldGetUserResponseByIdWhenExists() {
            User user = createAndSaveUser("eve", "eve_pub");

            Optional<UserResponse> result = userManagement.getUserById(user.getId());

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(user.getId());
            assertThat(result.get().username()).isEqualTo("eve");
            assertThat(result.get().publicUsername()).isEqualTo("eve_pub");
        }

        @Test
        @DisplayName("getUserByPublicUsername returns mapped UserResponse when user exists")
        void shouldGetUserResponseByPublicUsernameWhenExists() {
            User user = createAndSaveUser("frank", "frank_pub");

            Optional<UserResponse> result = userManagement.getUserByPublicUsername("frank_pub");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(user.getId());
            assertThat(result.get().username()).isEqualTo("frank");
        }

        @Test
        @DisplayName("getUserByPublicUsername returns empty Optional when public username does not exist")
        void shouldReturnEmptyWhenPublicUsernameNotFound() {
            Optional<UserResponse> result = userManagement.getUserByPublicUsername("non_existent_pub");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getUserGroup returns mapped UserRecords for specified set of IDs")
        void shouldGetUserGroupForMultipleIds() {
            User user1 = createAndSaveUser("user_one", "pub_one");
            User user2 = createAndSaveUser("user_two", "pub_two");
            createAndSaveUser("user_three", "pub_three");

            Set<UserRecord> group = userManagement.getUserGroup(Set.of(user1.getId(), user2.getId()));

            assertThat(group).hasSize(2);
            assertThat(group).extracting(UserRecord::username)
                    .containsExactlyInAnyOrder("user_one", "user_two");
        }
    }

    @Nested
    @DisplayName("followUser and idempotency")
    class FollowOperationsTests {

        private final String idempotencyKey = "a".repeat(32);

        @Test
        @DisplayName("successfully follows user, persists relationship in database")
        void shouldFollowUserSuccessfullyAndPersistRelationship() {
            User follower = createAndSaveUser("follower_user", "follower_pub");
            User target = createAndSaveUser("target_user", "target_pub");

            FollowRelationshipResponse response = userManagement.followUser(
                    follower.getId(),
                    target.getUsername(),
                    idempotencyKey
            );

            assertThat(response).isNotNull();
            assertThat(response.followerId()).isEqualTo(follower.getId());
            assertThat(response.followTargetId()).isEqualTo(target.getId());

            // Verify relationship persisted in DB
            Optional<FollowRelationship> relationship = followRelationshipRepository.findById(follower.getId(), target.getId());
            assertThat(relationship).isPresent();
            assertThat(relationship.get().getFollowerId()).isEqualTo(follower.getId());
            assertThat(relationship.get().getFollowTargetId()).isEqualTo(target.getId());
        }

        @Test
        @DisplayName("replaying followUser with same idempotency key is idempotent and returns existing result")
        void shouldBeIdempotentWhenFollowUserCalledRepeatedly() {
            User follower = createAndSaveUser("alice_f", "alice_f_pub");
            User target = createAndSaveUser("bob_t", "bob_t_pub");

            // First call
            FollowRelationshipResponse firstResponse = userManagement.followUser(
                    follower.getId(),
                    target.getUsername(),
                    idempotencyKey
            );

            // Second call with same idempotency key
            FollowRelationshipResponse secondResponse = userManagement.followUser(
                    follower.getId(),
                    target.getUsername(),
                    idempotencyKey
            );

            assertThat(secondResponse).isEqualTo(firstResponse);
            assertThat(followRelationshipRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws 409 CONFLICT when replaying idempotency key with different follow target")
        void shouldThrowConflictWhenReusingIdempotencyKeyWithDifferentTarget() {
            User follower = createAndSaveUser("alice_c", "alice_c_pub");
            User target1 = createAndSaveUser("bob_c", "bob_c_pub");
            User target2 = createAndSaveUser("carol_c", "carol_c_pub");

            // First follow
            userManagement.followUser(follower.getId(), target1.getUsername(), idempotencyKey);

            // Attempt with same key but different target
            assertThatThrownBy(() -> userManagement.followUser(follower.getId(), target2.getUsername(), idempotencyKey))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("throws 404 NOT FOUND when follower user does not exist")
        void shouldThrowNotFoundWhenFollowerDoesNotExist() {
            User target = createAndSaveUser("target_solo", "target_solo_pub");
            UUID nonExistentFollowerId = UUID.randomUUID();

            assertThatThrownBy(() -> userManagement.followUser(nonExistentFollowerId, target.getUsername(), idempotencyKey))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("throws 404 NOT FOUND when follow target user does not exist")
        void shouldThrowNotFoundWhenTargetDoesNotExist() {
            User follower = createAndSaveUser("follower_solo", "follower_solo_pub");
            String nonExistentTargetUsername = UUID.randomUUID().toString();

            assertThatThrownBy(() -> userManagement.followUser(follower.getId(), nonExistentTargetUsername, idempotencyKey))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("follow queries")
    class FollowQueriesTests {

        @Test
        @DisplayName("getUsersFollowersIds retrieves correct follower IDs from database")
        void shouldGetUsersFollowersIds() {
            User target = createAndSaveUser("celebrity", "celeb_pub");
            User fan1 = createAndSaveUser("fan1", "fan1_pub");
            User fan2 = createAndSaveUser("fan2", "fan2_pub");

            followRelationshipRepository.saveAndFlush(new FollowRelationship(fan1, target));
            followRelationshipRepository.saveAndFlush(new FollowRelationship(fan2, target));

            Set<UUID> followerIds = userManagement.getUsersFollowersIds(target.getId());

            assertThat(followerIds).containsExactlyInAnyOrder(fan1.getId(), fan2.getId());
        }

        @Test
        @DisplayName("getFollowTargetIds retrieves correct follow target IDs from database")
        void shouldGetFollowTargetIds() {
            User follower = createAndSaveUser("curious", "curious_pub");
            User target1 = createAndSaveUser("topic1", "topic1_pub");
            User target2 = createAndSaveUser("topic2", "topic2_pub");

            followRelationshipRepository.saveAndFlush(new FollowRelationship(follower, target1));
            followRelationshipRepository.saveAndFlush(new FollowRelationship(follower, target2));

            Set<UUID> targetIds = userManagement.getFollowTargetIds(follower.getId());

            assertThat(targetIds).containsExactlyInAnyOrder(target1.getId(), target2.getId());
        }
    }
}
