package dev.e66e.social_app_api;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisTestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName
                    .parse("redis:8.10.2-alpine"))
                    .withExposedPorts(6379);

    static {
        redis.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.data.redis.host=" + redis.getHost(),
                "spring.data.redis.port=" + redis.getMappedPort(6379)
        ).applyTo(ctx.getEnvironment());
    }
}
