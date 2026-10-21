package dev.e66e.social_app_api.comments;

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
class CommentEntityTest {

    @Autowired
    CommentRepository commentRepository;

    @Test
    @Transactional
    @DisplayName("Testing Post entity mapping with database")
    void commentEntityTest() {
        // Given
        Comment comment = new Comment();
        UUID postUUID = UUID.randomUUID();
        UUID commentUUID = UUID.randomUUID();

        comment.setId(new CommentId(commentUUID, postUUID));
        comment.setContent("Test content");
        comment.setParentCommentId(null);

        // When
        Comment dbComment = commentRepository.save(comment);

        // Then
        assertNotNull(dbComment);
        assertAll(() -> {
            assertNotNull(dbComment.getId());
           assertEquals(postUUID, dbComment.getId().getPostId());
           assertEquals("Test content", dbComment.getContent());
           assertNull(dbComment.getParentCommentId());
        });
    }
}