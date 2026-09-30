package dev.e66e.social_app_api.users.persistence;

import dev.e66e.social_app_api.PostgresTestcontainers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@PostgresTestcontainers
@DisplayName("UserEntity test")
public class UserEntityTest {

    @Autowired
    UserRepository userRepository;

    @Test
    @Transactional
    @DisplayName("Testing User entity mapping with database")
    void userEntitySave() {
        User user = new User();

        user.setUsername("testusername");
        user.setPublicUsername("testpublicusername");
        user.setAvatarUrl("testavatarurl");
        user.setBio("testbio");
        user.setRole(UserRole.USER);
        User dbUser = userRepository.saveAndFlush(user);

        assertAll(() -> {
            assertNotNull(dbUser.getId());
            assertEquals("testusername", dbUser.getUsername());
            assertEquals("testpublicusername", dbUser.getPublicUsername());
            assertEquals("testavatarurl", dbUser.getAvatarUrl());
            assertEquals("testbio", dbUser.getBio());
            assertEquals(UserRole.USER, dbUser.getRole());
        });
    }
}
