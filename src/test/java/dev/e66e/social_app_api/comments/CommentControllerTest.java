package dev.e66e.social_app_api.comments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import static org.mockito.Mockito.*;

@WebMvcTest(CommentController.class)
@TestPropertySource(properties = "comments.default-page-size=10")
@AutoConfigureRestTestClient
class CommentControllerTest {

    @MockitoBean
    private CommentExternalAPI api;

    @Autowired
    private RestTestClient restClient;

    @Value("${comments.default-page-size}")
    private int defaultPageSize;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    class GetSingleComment {

        @Test
        @WithMockUser
        void getComment_returnsComment() {
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            CommentResponse response = new CommentResponse(commentId,
                    postId,
                    "hello",
                    null,
                    Instant.now(),
                    null);
            when(api.getComment(commentId, postId)).thenReturn(Optional.of(response));

            restClient.get().uri("/api/comments/{postId}/{commentId}", postId, commentId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(CommentResponse.class)
                    .consumeWith(result -> {
                        CommentResponse cr = result.getResponseBody();
                        assertThat(cr).isNotNull();
                        assertThat(cr.id()).isEqualTo(commentId);
                        assertThat(cr.postId()).isEqualTo(postId);
                        assertThat(cr.content()).isEqualTo("hello");
                    });
        }

        @Test
        @WithMockUser
        void getComment_returns404_whenNotFound() {
            when(api.getComment(any(), any())).thenReturn(Optional.empty());

            restClient.get().uri("/api/comments/{postId}/{commentId}",
                            UUID.randomUUID(), UUID.randomUUID())
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    class GetComments {

        @Test
        @WithMockUser
        void postComments_forwardsCursorAndLimit() {
            UUID postId = UUID.randomUUID();
            when(api.getPostComments(postId, "abc", 5))
                    .thenReturn(new CommentsPageResponse(List.of(), "next"));

            restClient.get().uri(builder -> builder
                        .path("/api/comments/{id}")
                        .queryParam("cursor", "abc")
                        .queryParam("limit", 5)
                        .build(postId))
                    .exchange().expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.nextCursor").isEqualTo("next");

            verify(api).getPostComments(postId, "abc", 5);
        }

        @Test
        @WithMockUser
        void postComments_usesDefaultLimit_whenNotProvided() {
            UUID postId = UUID.randomUUID();
            when(api.getPostComments(postId, null, defaultPageSize))
                    .thenReturn(new CommentsPageResponse(List.of(), null));

            restClient.get()
                    .uri("/api/comments/{id}", postId)
                    .exchange().expectStatus().isOk();

            verify(api).getPostComments(postId, null, defaultPageSize);
        }

        @Test
        @WithMockUser
        void userComments_usesDefaultLimit_whenNotProvided() {
            UUID userId = UUID.randomUUID();
            when(api.getUserComments(userId, null, defaultPageSize))
                    .thenReturn(new CommentsPageResponse(List.of(), null));

            restClient.get()
                    .uri("/api/comments/users/{userId}", userId)
                    .exchange()
                    .expectStatus().isOk();

            verify(api).getUserComments(userId, null, defaultPageSize);
        }
    }

    @Nested
    class PostComment {

        @Test
        @WithMockUser
        void postComment_createsCommentWithAuthorFromJwt() {
            UUID userId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            String idempotencyKey = "a".repeat(32);
            CommentResponse response = new CommentResponse(commentId, postId, "hello", null, Instant.now(), null);
            CreateCommentRequest request = new CreateCommentRequest(
                    idempotencyKey,
                    new CommentRequest(postId,
                            null,
                            "hello"));

            when(api.postComment(eq(userId), eq(idempotencyKey), any(CommentRequest.class)))
                    .thenReturn(response);

            Jwt mockJwt = Jwt.withTokenValue("dummy-token")
                    .header("alg", "none")
                    .subject(userId.toString())
                    .build();

            when(jwtDecoder.decode("dummy-token")).thenReturn(mockJwt);

            restClient.post()
                    .uri("/api/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer dummy-token")
                    .body(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(CommentResponse.class)
                    .consumeWith(result -> {
                        assertThat(result.getResponseBody()).isNotNull();
                        CommentResponse cr = result.getResponseBody();
                        assertThat(cr).isNotNull();
                        assertThat(cr.id()).isEqualTo(commentId);
                    });

            verify(api).postComment(eq(userId), eq(idempotencyKey), any(CommentRequest.class));
        }
    }
}
