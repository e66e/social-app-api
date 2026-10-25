package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.User;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Nested
    class UserToUserRecord {

        @Test
        void userToUserRecord_shouldMapAllFields() {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setUsername("alice");
            user.setPublicUsername("alice_public");
            user.setAvatarUrl("https://example.com/avatar.png");
            user.setBio("hello");
            user.setActive(true);
            user.setHotUser(false);
            user.setRole(UserRole.USER);


            UserRecord record = mapper.userToUserRecord(user);

            assertThat(record.id()).isEqualTo(user.getId());
            assertThat(record.username()).isEqualTo("alice");
            assertThat(record.publicUsername()).isEqualTo("alice_public");
            assertThat(record.avatarUrl()).isEqualTo("https://example.com/avatar.png");
            assertThat(record.bio()).isEqualTo("hello");
            assertThat(record.isActive()).isTrue();
            assertThat(record.isHotUser()).isFalse();
            assertThat(record.role()).isEqualTo(UserRole.USER);
        }

        @Test
        void userToUserRecord_shouldHandleNullAvatarAndBio() {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setUsername("bob");
            user.setPublicUsername("bob_public");
            user.setAvatarUrl(null);
            user.setBio(null);
            user.setActive(true);
            user.setHotUser(true);

            UserRecord record = mapper.userToUserRecord(user);

            assertThat(record.avatarUrl()).isEmpty();
            assertThat(record.bio()).isEmpty();
        }
    }

    @Nested
    class UserRecordToUserResponse {

        @Test
        void userRecordToUserResponse_shouldMapAllFields() {
            UserRecord record = new UserRecord(
                    UUID.randomUUID(), "carol", "carol_public",
                    "https://example.com/c.png", "hi",
                    true, UserRole.ADMIN, false
            );

            UserResponse response = mapper.userRecordToUserResponse(record);

            assertThat(response.id()).isEqualTo(record.id());
            assertThat(response.username()).isEqualTo("carol");
            assertThat(response.publicUsername()).isEqualTo("carol_public");
            assertThat(response.avatarUrl()).isEqualTo("https://example.com/c.png");
            assertThat(response.bio()).isEqualTo("hi");
            // isActive, role, isHotUser are intentionally not in UserResponse
        }

        @Test
        void userRecordToUserResponse_shouldHandleNullAvatarAndBio() {
            UserRecord record = new UserRecord(
                    UUID.randomUUID(), "dave", "dave_public",
                    null, null, false, UserRole.USER, false
            );

            UserResponse response = mapper.userRecordToUserResponse(record);

            assertThat(response.avatarUrl()).isEmpty();
            assertThat(response.bio()).isEmpty();
        }
    }
}
