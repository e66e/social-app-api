package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.idempotency.FollowRelationshipIdempotencyAPI;
import dev.e66e.social_app_api.users.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagement Unit Tests")
class UserManagementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRelationshipRepository followRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FollowRelationshipMapper followMapper;

    @Mock
    private FollowRelationshipHasher followHasher;

    @Mock
    private FollowRelationshipIdempotencyAPI followIdempotencyAPI;

    @Mock
    private ApplicationEventPublisher events;

    private UserManagement userManagement;

    @BeforeEach
    void setUp() {
        userManagement = new UserManagement(
                userRepository,
                followRepository,
                userMapper,
                followMapper,
                followHasher,
                followIdempotencyAPI,
                events
        );
    }

    @Nested
    @DisplayName("followUser")
    class FollowUser {

        private final UUID followerId = UUID.randomUUID();
        private final UUID followTargetId = UUID.randomUUID();
        private final String idempotencyKey = "a".repeat(32);
        private final String relationHash = "b".repeat(64);

        @Test
        @DisplayName("successfully follows user using idempotency API and returns mapped response")
        void followUser_success() {
            // Given
            User follower = User.builder().id(followerId).username("follower").build();
            User followTarget = User.builder().id(followTargetId).username("target").build();
            FollowRelationship relationship = new FollowRelationship(follower, followTarget);
            FollowRelationshipResponse expectedResponse = new FollowRelationshipResponse(followerId, followTargetId);

            when(followHasher.createRelationshipHash(followerId, followTargetId)).thenReturn(relationHash);
            when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
            when(userRepository.findById(followTargetId)).thenReturn(Optional.of(followTarget));
            when(followIdempotencyAPI.executeIdempotent(eq(followerId), eq(idempotencyKey), eq(relationHash), any()))
                    .thenReturn(followTargetId);
            when(followRepository.findById(followerId, followTargetId)).thenReturn(Optional.of(relationship));
            when(followMapper.followRelationshipToFollowRelationshipResponse(relationship)).thenReturn(expectedResponse);

            // When
            FollowRelationshipResponse actualResponse = userManagement.followUser(followerId, followTargetId, idempotencyKey);

            // Then
            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(followHasher).createRelationshipHash(followerId, followTargetId);
            verify(followIdempotencyAPI).executeIdempotent(eq(followerId), eq(idempotencyKey), eq(relationHash), any());
            verify(followRepository).findById(followerId, followTargetId);
        }

        @Test
        @DisplayName("supplier callback saves relationship and returns follow target ID")
        void followUser_callbackExecutionPersistsRelationship() {
            // Given
            User follower = User.builder().id(followerId).username("follower").build();
            User followTarget = User.builder().id(followTargetId).username("target").build();
            FollowRelationship relationship = new FollowRelationship(follower, followTarget);
            FollowRelationshipResponse expectedResponse = new FollowRelationshipResponse(followerId, followTargetId);

            when(followHasher.createRelationshipHash(followerId, followTargetId)).thenReturn(relationHash);
            when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
            when(userRepository.findById(followTargetId)).thenReturn(Optional.of(followTarget));
            when(followRepository.saveAndFlush(any(FollowRelationship.class))).thenReturn(relationship);
            when(followRepository.findById(followerId, followTargetId)).thenReturn(Optional.of(relationship));
            when(followMapper.followRelationshipToFollowRelationshipResponse(relationship)).thenReturn(expectedResponse);

            // Delegate to the callback supplied to executeIdempotent
            when(followIdempotencyAPI.executeIdempotent(eq(followerId), eq(idempotencyKey), eq(relationHash), any()))
                    .thenAnswer(inv -> {
                        Supplier<UUID> supplier = inv.getArgument(3);
                        return supplier.get();
                    });

            // When
            userManagement.followUser(followerId, followTargetId, idempotencyKey);

            // Then
            ArgumentCaptor<FollowRelationship> captor = ArgumentCaptor.forClass(FollowRelationship.class);
            verify(followRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getFollowerId()).isEqualTo(followerId);
            assertThat(captor.getValue().getFollowTargetId()).isEqualTo(followTargetId);
        }

        @Test
        @DisplayName("throws 404 Not Found when follower does not exist")
        void followUser_followerNotFound_throws404() {
            // Given
            when(followHasher.createRelationshipHash(followerId, followTargetId)).thenReturn(relationHash);
            when(userRepository.findById(followerId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userManagement.followUser(followerId, followTargetId, idempotencyKey))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(followIdempotencyAPI, never()).executeIdempotent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws 404 Not Found when follow target does not exist")
        void followUser_targetNotFound_throws404() {
            // Given
            User follower = User.builder().id(followerId).username("follower").build();
            when(followHasher.createRelationshipHash(followerId, followTargetId)).thenReturn(relationHash);
            when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
            when(userRepository.findById(followTargetId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userManagement.followUser(followerId, followTargetId, idempotencyKey))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(followIdempotencyAPI, never()).executeIdempotent(any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws 500 Internal Server Error when record was added but cannot be found in database")
        void followUser_recordNotFoundAfterSave_throws500() {
            // Given
            User follower = User.builder().id(followerId).username("follower").build();
            User followTarget = User.builder().id(followTargetId).username("target").build();

            when(followHasher.createRelationshipHash(followerId, followTargetId)).thenReturn(relationHash);
            when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
            when(userRepository.findById(followTargetId)).thenReturn(Optional.of(followTarget));
            when(followIdempotencyAPI.executeIdempotent(eq(followerId), eq(idempotencyKey), eq(relationHash), any()))
                    .thenReturn(followTargetId);
            when(followRepository.findById(followerId, followTargetId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userManagement.followUser(followerId, followTargetId, idempotencyKey))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("persists new user, publishes UserCreatedEvent, and returns UserRecord")
        void register_success() {
            UUID id = UUID.randomUUID();
            RegistrationData regData = new RegistrationData(id,
                    "alice",
                    "alice_pub",
                    "https://avatar.url");

            User savedUser = User.builder()
                    .id(id)
                    .username("alice")
                    .publicUsername("alice_pub")
                    .avatarUrl("https://avatar.url")
                    .bio("")
                    .isActive(true)
                    .role(UserRole.USER)
                    .isHotUser(false).build();

            UserRecord record = new UserRecord(id,
                    "alice",
                    "alice_pub",
                    "https://avatar.url",
                    null,
                    true,
                    UserRole.USER,
                    false);

            when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
            when(userMapper.userToUserRecord(savedUser)).thenReturn(record);

            UserRecord result = userManagement.register(regData);

            assertThat(result).isEqualTo(record);
            verify(userRepository).saveAndFlush(any(User.class));
            verify(events).publishEvent(any(UserCreatedEvent.class));
        }
    }

    @Nested
    @DisplayName("isUsernameAvailable")
    class IsUsernameAvailable {

        @Test
        @DisplayName("returns true when username is not taken")
        void isUsernameAvailable_available() {
            when(userRepository.existsByUsername("unique_name")).thenReturn(false);

            assertThat(userManagement.isUsernameAvailable("unique_name")).isTrue();
        }

        @Test
        @DisplayName("returns false when username already exists")
        void isUsernameAvailable_taken() {
            when(userRepository.existsByUsername("existing_name")).thenReturn(true);

            assertThat(userManagement.isUsernameAvailable("existing_name")).isFalse();
        }
    }

    @Nested
    @DisplayName("getUser queries")
    class GetUserQueries {

        @Test
        @DisplayName("getUser returns mapped UserRecord when user exists")
        void getUser_found() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("bob").build();
            UserRecord record = new UserRecord(userId, "bob", "bob_pub", null, null, true, UserRole.USER, false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.userToUserRecord(user)).thenReturn(record);

            assertThat(userManagement.getUser(userId)).contains(record);
        }

        @Test
        @DisplayName("getUser returns empty Optional when user does not exist")
        void getUser_notFound() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThat(userManagement.getUser(userId)).isEmpty();
        }

        @Test
        @DisplayName("getUserById returns mapped UserResponse when found")
        void getUserById_found() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("bob").build();
            UserRecord record = new UserRecord(userId, "bob", "bob_pub", null, null, true, UserRole.USER, false);
            UserResponse response = new UserResponse(userId, "bob", "bob_pub", null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.userToUserRecord(user)).thenReturn(record);
            when(userMapper.userRecordToUserResponse(record)).thenReturn(response);

            assertThat(userManagement.getUserById(userId)).contains(response);
        }

        @Test
        @DisplayName("getUserByPublicUsername returns mapped UserResponse when found")
        void getUserByPublicUsername_found() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("bob").publicUsername("bob_pub").build();
            UserRecord record = new UserRecord(userId, "bob", "bob_pub", null, null, true, UserRole.USER, false);
            UserResponse response = new UserResponse(userId, "bob", "bob_pub", null, null);

            when(userRepository.findByPublicUsername("bob_pub")).thenReturn(Optional.of(user));
            when(userMapper.userToUserRecord(user)).thenReturn(record);
            when(userMapper.userRecordToUserResponse(record)).thenReturn(response);

            assertThat(userManagement.getUserByPublicUsername("bob_pub")).contains(response);
        }
    }

    @Nested
    @DisplayName("Follow queries")
    class FollowQueries {

        @Test
        @DisplayName("getUsersFollowersIds extracts and returns follower UUIDs")
        void getUsersFollowersIds() {
            UUID targetId = UUID.randomUUID();
            UUID f1 = UUID.randomUUID();
            UUID f2 = UUID.randomUUID();

            User follower1 = User.builder().id(f1).build();
            User follower2 = User.builder().id(f2).build();
            User target = User.builder().id(targetId).build();

            FollowRelationship rel1 = new FollowRelationship(follower1, target);
            FollowRelationship rel2 = new FollowRelationship(follower2, target);

            when(followRepository.findById_followTargetId(targetId)).thenReturn(Set.of(rel1, rel2));

            Set<UUID> followers = userManagement.getUsersFollowersIds(targetId);

            assertThat(followers).containsExactlyInAnyOrder(f1, f2);
        }

        @Test
        @DisplayName("getFollowTargetIds extracts and returns follow target UUIDs")
        void getFollowTargetIds() {
            UUID followerId = UUID.randomUUID();
            UUID t1 = UUID.randomUUID();

            User follower = User.builder().id(followerId).build();
            User target1 = User.builder().id(t1).build();

            FollowRelationship rel1 = new FollowRelationship(follower, target1);

            when(followRepository.findById_followerId(followerId)).thenReturn(Set.of(rel1));

            Set<UUID> targets = userManagement.getFollowTargetIds(followerId);

            assertThat(targets).containsExactly(t1);
        }

        @Test
        @DisplayName("getUserGroup returns mapped UserRecords for requested IDs")
        void getUserGroup() {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();
            User user1 = User.builder().id(u1).username("user1").build();
            User user2 = User.builder().id(u2).username("user2").build();
            UserRecord rec1 = new UserRecord(u1, "user1", "u1", null, null, true, UserRole.USER, false);
            UserRecord rec2 = new UserRecord(u2, "user2", "u2", null, null, true, UserRole.USER, false);

            when(userRepository.findByIdIn(Set.of(u1, u2))).thenReturn(Set.of(user1, user2));
            when(userMapper.userToUserRecord(user1)).thenReturn(rec1);
            when(userMapper.userToUserRecord(user2)).thenReturn(rec2);

            Set<UserRecord> group = userManagement.getUserGroup(Set.of(u1, u2));

            assertThat(group).containsExactlyInAnyOrder(rec1, rec2);
        }
    }
}
