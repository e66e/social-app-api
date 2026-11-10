package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.config.OnboardingStatusProvider;
import dev.e66e.social_app_api.users.UserInternalAPI;
import dev.e66e.social_app_api.users.UserRecord;
import dev.e66e.social_app_api.users.persistence.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ApplicationModuleTest
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@PostgresTestcontainers
@AutoConfigureMockMvc
class PostsModuleSliceTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private PostRepository postRepo;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserInternalAPI userAPI;

    @MockitoBean
    private OnboardingStatusProvider onboardingStatusProvider;


    // Mocking Spring MVC with resource server to accept simple custom jwt
    @TestConfiguration
    static class MockJwt {
        static final UUID USER_ID = UUID.randomUUID();
        static final String TOKEN = "test-token";

        @Bean
        RestTestClient restTestClient(MockMvc mockMvc) {
            return RestTestClient.bindTo(mockMvc).build();
        }

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
            return http.build();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!TOKEN.equals(token)) {
                    throw new JwtException("Unknown token.");
                }
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject(USER_ID.toString())
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build();
            };
        }

        @Bean
        OAuth2TokenValidator<Jwt> jwtValidator() {
            return JwtValidators.createDefault();
        }
    }

    @BeforeEach
    void setUp() {
        postRepo.deleteAll();
        jdbcTemplate.update("DELETE FROM post_creation_request");
        UserRecord mockUser = new UserRecord(
                MockJwt.USER_ID,
                "testuser",
                "testuser_pub",
                "https://avatar.url",
                "bio",
                true,
                UserRole.USER,
                false
        );
        when(userAPI.getUser(eq(mockUser.id()))).thenReturn(Optional.of(mockUser));
        when(userAPI.getUser(any())).thenAnswer(i -> Optional.of(new UserRecord(
                i.getArgument(0),
                "userUser2",
                "testUser2_pub",
                "https://avatar.url",
                "bio",
                true,
                UserRole.USER,
                false
        )));
        lenient().when(onboardingStatusProvider.isUserOnboarded(any())).thenReturn(true);
    }

    @Nested
    class GetEndpoint {

        final UUID userId = UUID.randomUUID();
        final String textContent = "textContent";
        final String imageUrl = "https://example.com/images";

        @Test
        void whenPostExists_thenSuccess() {
            // Given
            Post postFromDb = postRepo.saveAndFlush(new Post(userId, textContent, imageUrl));

            // When / Then
            client.get()
                    .uri("/api/posts/" + postFromDb.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PostResponse.class)
                    .value(post -> {
                        assertNotNull(post);
                        assertEquals(postFromDb.getId(), post.postId());
                        assertEquals(postFromDb.getAuthorId(), post.authorId());
                        assertEquals(postFromDb.getTextContent(), post.textContent());
                        assertEquals(postFromDb.getImageUrl(), post.imageUrl());
                        assertNotNull(post.createdAt());
                        assertNull(post.updatedAt());
                    });
        }

        @Test
        void whenPostDoesntExist_thenFailure() {
            // Given
            final UUID postId = UUID.randomUUID();

            // When / Then
            client.get()
                    .uri("/api/posts/" + postId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    class PostEndpoint {

        final String textContent = "textContent";
        final String imageUrl = null;
        final String idempotencyKey = "idempotencyKeyidempotencyKeyidempotencyKey";
        final PostRequest pr = new PostRequest(textContent, imageUrl);
        final CreatePostRequest cpr = new CreatePostRequest(idempotencyKey, pr);

        @Test
        void whenEverythingCorrect_thenSuccess() {
            // When / Then
            client.post()
                    .uri("/api/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + MockJwt.TOKEN)
                    .body(cpr)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PostResponse.class)
                    .value(post -> {
                        assertNotNull(post);
                        assertEquals(pr.content(), post.textContent());
                        assertEquals("", post.imageUrl());
                        assertNotNull(post.createdAt());
                        assertNull(post.updatedAt());
                    });

            assertEquals(1, postRepo.findAll().size());
        }

        @Test
        void whenPostingWithSameContentAndSameIdempotencyKey_thenReturnFirstPost() {
            // Given
            PostResponse responseBody = restTestClient.post()
                    .uri("/api/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + MockJwt.TOKEN)
                    .body(cpr)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectBody(PostResponse.class)
                    .returnResult().getResponseBody();

            assertNotNull(responseBody);

            // When / Then
            restTestClient.post()
                    .uri("/api/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + MockJwt.TOKEN)
                    .body(cpr)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PostResponse.class)
                    .value(post -> {
                        assertNotNull(post);
                        assertEquals(responseBody, post);
                    });

            assertEquals(1, postRepo.findAll().size());
        }

        @Test
        void whenSameIdempotencyKeyUsedForDifferentPost_returnConflictStatus() {
            // Given
            PostResponse responseBody = restTestClient.post()
                    .uri("/api/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + MockJwt.TOKEN)
                    .body(cpr)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectBody(PostResponse.class)
                    .returnResult().getResponseBody();

            assertNotNull(responseBody);

            // When / Then
            restTestClient.post()
                    .uri("/api/posts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + MockJwt.TOKEN)
                    .body(new CreatePostRequest(idempotencyKey, new PostRequest("ipsum lorem", null)))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().is4xxClientError();

            assertEquals(1, postRepo.findAll().size());
        }
    }
}
