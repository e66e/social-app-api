package dev.e66e.social_app_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles({"test"})
@ContextConfiguration(initializers = {KeycloakTestcontainersInitializer.class, PostgresTestcontainersInitializer.class})
class SocialAppAPIApplicationTests {

	@Test
	void contextLoads() {
	}
}
