package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.PostgresTestcontainers;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.modulith.test.ApplicationModuleTest;

import static org.junit.jupiter.api.Assertions.*;

@ApplicationModuleTest
@PostgresTestcontainers
@AutoConfigureRestTestClient
class PostsModuleSliceTest {


}