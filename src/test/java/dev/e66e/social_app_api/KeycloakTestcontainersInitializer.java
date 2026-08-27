package dev.e66e.social_app_api;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

public class KeycloakTestcontainersInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @SuppressWarnings("resource")
    static final GenericContainer<?> keycloak =
            new GenericContainer<>(DockerImageName
                .parse("quay.io/keycloak/keycloak:26.7.1"))
                .withEnv(Map.of(
                        "KC_BOOTSTRAP_ADMIN_USERNAME", "test",
                        "KC_BOOTSTRAP_ADMIN_PASSWORD", "test"
                ))
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource(
                                "test-realm.json"),
                                "/opt/keycloak/data/import/social-app-realm.json")
                .withExposedPorts(8080)
                .withCommand("start-dev --import-realm");

    static {
        keycloak.start();
        System.out.println(keycloak.getLogs());
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "keycloak.exposed-port=" + keycloak.getMappedPort(8080)
        ).applyTo(ctx.getEnvironment());
    }
}
