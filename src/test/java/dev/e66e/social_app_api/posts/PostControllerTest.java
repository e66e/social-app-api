package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.onboarding.OnboardingInternalAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(PostController.class)
@TestPropertySource(properties = {
        "posts.default-page-size=10",
        "logging.level.org.springframework.web=DEBUG",
        "logging.level.org.springframework.security=DEBUG",
        "logging.level.org.springframework.web.servlet.mvc.method.annotation=DEBUG"
})
@AutoConfigureRestTestClient
class PostControllerTest {

    @MockitoBean
    private PostExternalAPI postAPI;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private OnboardingInternalAPI onboardingAPI;

    @Autowired
    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        lenient().when(onboardingAPI.isUserOnboarded(any())).thenReturn(true);
    }

    @Nested
    class GetSinglePost {

        @Test
        @WithMockUser
        void shouldReturnPostWhenFound() {
            UUID postId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            PostResponse response = new PostResponse(
                    postId,
                    authorId,
                    "alice",
                    Instant.now(),
                    null,
                    "Hello world",
                    "https://example.com/img.png"
            );

            when(postAPI.getPost(postId)).thenReturn(Optional.of(response));

            restClient.get().uri("/api/posts/{id}", postId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PostResponse.class)
                    .consumeWith(result -> assertThat(result.getResponseBody())
                        .isNotNull()
                        .returns(postId, PostResponse::postId)
                        .returns(authorId, PostResponse::authorId)
                        .satisfies(p -> {
                            assertThat(p.authorPubUsername()).isNotNull().isEqualTo("alice");
                            assertThat(p.textContent()).isEqualTo("Hello world");
                        }));
        }

        @Test
        @WithMockUser
        void shouldReturn404WhenPostNotFound() {
            UUID postId = UUID.randomUUID();
            when(postAPI.getPost(postId)).thenReturn(Optional.empty());

            restClient.get().uri("/api/posts/{id}", postId)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    class CreatePost {

        @Test
        void shouldCreatePostWithAuthenticatedUser() {
            UUID authorId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            String idempotencyKey = "a".repeat(33);
            PostRequest pr = new PostRequest("New post content", null);
            CreatePostRequest request = new CreatePostRequest(idempotencyKey, pr);

            PostResponse response = new PostResponse(
                    postId,
                    authorId,
                    "alice",
                    Instant.now(),
                    null,
                    "New post content",
                    ""
            );

            when(postAPI.createPost(eq(authorId), eq(idempotencyKey), any(PostRequest.class)))
                    .thenReturn(response);

            Jwt mockJwt = Jwt.withTokenValue("mock-jwt-token")
                    .header("alg", "none")
                    .subject(authorId.toString())
                    .build();

            when(jwtDecoder.decode("mock-jwt-token")).thenReturn(mockJwt);

            restClient.post()
                    .uri("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer mock-jwt-token")
                    .body(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(PostResponse.class)
                    .consumeWith(result -> {
                        PostResponse res = result.getResponseBody();
                        assertThat(res).isNotNull();
                        assertThat(res.postId()).isEqualTo(postId);
                        assertThat(res.textContent()).isEqualTo("New post content");
                    });

            verify(postAPI).createPost(eq(authorId), eq(idempotencyKey), any(PostRequest.class));
        }
    }

    @Nested
    class GetUserPosts {

        @Test
        @WithMockUser
        void shouldReturnUserPostsWithPagination() {
            UUID userId = UUID.randomUUID();
            PostsPageResponse response = new PostsPageResponse(List.of(), "next-cursor");

            when(postAPI.getUsersPost(userId, "cursor123", 5)).thenReturn(response);

            restClient.get().uri(builder -> builder
                            .path("/api/posts/users/{userId}")
                            .queryParam("cursor", "cursor123")
                            .queryParam("limit", 5)
                            .build(userId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.nextCursor").isEqualTo("next-cursor");

            verify(postAPI).getUsersPost(userId, "cursor123", 5);
        }
    }
}
