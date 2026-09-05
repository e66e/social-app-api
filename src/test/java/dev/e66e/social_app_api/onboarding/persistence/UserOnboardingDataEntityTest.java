package dev.e66e.social_app_api.onboarding.persistence;

import dev.e66e.social_app_api.PostgresTestcontainersInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(initializers = PostgresTestcontainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserOnboardingDataEntity test.")
class UserOnboardingDataEntityTest {

    @Autowired
    UserOnboardingRepository repo;

    @Test
    @DisplayName("Testing UserOnboardingData entity mapping with database")
    void userOnboardingDataEntityTest() {
        UserOnboardingData uod = new UserOnboardingData();
        UUID id = UUID.randomUUID();

        uod.setId(id);
        uod.setOnboardingStatus(UserOnboardingStatus.PENDING);
        repo.save(uod);

        UserOnboardingData dbUod = repo.getReferenceById(uod.getId());
        assertEquals(id, dbUod.getId());
        assertEquals(UserOnboardingStatus.PENDING, uod.getOnboardingStatus());
    }
}