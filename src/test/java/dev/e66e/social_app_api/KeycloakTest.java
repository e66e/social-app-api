package dev.e66e.social_app_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.client.RestTestClient;

@ActiveProfiles({"test"})
@ContextConfiguration(initializers = {KeycloakTestcontainersInitializer.class, PostgresTestcontainersInitializer.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class KeycloakTest {

    @Autowired
    private RestTestClient restClient;

    @Test
    void shouldFetchPublicKeyFromKeycloak(@Value("${keycloak.exposed-port}") int port) {
        String keysLink = "http://localhost:" + port + "/realms/social-app/protocol/openid-connect/certs";

        this.restClient.get()
                .uri(keysLink)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys").isArray()
                .jsonPath("$.keys").isNotEmpty();
    }
}
