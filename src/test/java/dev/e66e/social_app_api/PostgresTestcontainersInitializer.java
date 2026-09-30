package dev.e66e.social_app_api;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class PostgresTestcontainersInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.4")
            .withDatabaseName("social_app_db")
            .withUsername("test_admin")
            .withPassword("test_secret")
            .withExposedPorts(5432);

    static {
        postgres.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.test.database.replace=none"
        ).applyTo(ctx.getEnvironment());
    }
}
