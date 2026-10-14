package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.PostgresTestcontainersInitializer;
import dev.e66e.social_app_api.RedisTestcontainersInitializer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@ImportAutoConfiguration({
        JacksonAutoConfiguration.class,
        DataRedisAutoConfiguration.class,
        DataRedisRepositoriesAutoConfiguration.class
})
@ContextConfiguration(initializers = {
        PostgresTestcontainersInitializer.class,
        RedisTestcontainersInitializer.class
})
public @interface DataJpaAndRedis {
}
