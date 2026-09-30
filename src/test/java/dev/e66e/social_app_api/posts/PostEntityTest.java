package dev.e66e.social_app_api.posts;

import dev.e66e.social_app_api.PostgresTestcontainers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@PostgresTestcontainers
@DisplayName("PostEntity test.")
class PostEntityTest {

    @Autowired
    PostRepository postRepository;

    @Test
    @Transactional
    @DisplayName("Testing Post entity mapping with database")
    void postEntityTest() {
        Post post = new Post();
        UUID userUuid = UUID.randomUUID();

        post.setUserId(userUuid);
        post.setTextContent("Test content");
        post.setImageUrl("https://testimage.com");

        Post dbPost = postRepository.saveAndFlush(post);

        assertNotNull(dbPost);

        assertAll(() -> {
            assertNotNull(dbPost.getId());
            assertEquals(userUuid, dbPost.getUserId());
            assertEquals("Test content", dbPost.getTextContent());
            assertEquals("https://testimage.com", dbPost.getImageUrl());
            assertNotNull(dbPost.getCreatedAt());
            assertNull(dbPost.getUpdatedAt());
        });
    }
}