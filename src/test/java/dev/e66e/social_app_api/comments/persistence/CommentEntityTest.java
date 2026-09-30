package dev.e66e.social_app_api.comments.persistence;

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
        Comment comment = new Comment();
        UUID postUuid = UUID.randomUUID();

        comment.setPostId(postUuid);
        comment.setContent("Test content");
        comment.setParentCommentId(null);

        Comment dbComment = commentRepository.save(comment);

        assertNotNull(dbComment);
        assertAll(() -> {
            assertNotNull(dbComment.getId());
           assertEquals(postUuid, dbComment.getPostId());
           assertEquals("Test content", dbComment.getContent());
           assertNull(dbComment.getParentCommentId());
        });
    }
}