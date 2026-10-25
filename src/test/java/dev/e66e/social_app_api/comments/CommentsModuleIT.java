package dev.e66e.social_app_api.comments;


import dev.e66e.social_app_api.PostgresTestcontainers;
import dev.e66e.social_app_api.posts.PostInternalAPI;
import dev.e66e.social_app_api.posts.PostRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ModuleSlicing
@AutoConfigureMockMvc
@PostgresTestcontainers
class CommentsModuleIT {

    @MockitoBean
    private PostInternalAPI postAPI;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository repository;

    private UUID authorId;
    private UUID postId;

    @BeforeEach
    void setUp() {
        this.repository.deleteAll();
        this.authorId = UUID.randomUUID();
        this.postId = UUID.randomUUID();
    }

    @Test
    void shouldCreateCommentSuccessfully() throws Exception {
        when(postAPI.getPostRecord(postId)).thenReturn(Optional.of(
                new PostRecord(postId,
                        authorId,
                        Instant.now(),
                        null,
                        "Test Post",
                        "")
        ));

        CommentRequest commentRequest = new CommentRequest(postId,
                null,
                "This is an integration test comment.");
        String idempotencyKey = "a".repeat(32);
        CreateCommentRequest request = new CreateCommentRequest(idempotencyKey, commentRequest);

        this.mockMvc.perform(post("/api/comments")
                    .with(jwt().jwt(jwt -> jwt.subject(authorId.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId.toString()))
                .andExpect(jsonPath("$.content").value("This is an integration test comment."))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturnNotFoundWhenCommentingOnNonExistentPost() throws Exception {
        when(postAPI.getPostRecord(postId)).thenReturn(Optional.empty());

        CommentRequest commentRequest = new CommentRequest(postId, null, "This should fail.");
        String idempotencyKey = "a".repeat(32);
        CreateCommentRequest request = new CreateCommentRequest(idempotencyKey, commentRequest);

        mockMvc.perform(post("/api/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(authorId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFetchPostComments() throws Exception {
        // Seed database directly
        Comment comment = new Comment(postId, authorId, null, "Seeded comment content");
        repository.saveAndFlush(comment); // Saves comment and generates CommentId[cite: 1]

        mockMvc.perform(get("/api/comments/{id}", postId)
                        .param("limit", "10")
                        .with(jwt().jwt(jwt -> jwt.subject(authorId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].content").value("Seeded comment content"))
                .andExpect(jsonPath("$.comments[0].postId").value(postId.toString()));
    }
}
