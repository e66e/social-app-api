package dev.e66e.social_app_api.comments.persistence;

import dev.e66e.social_app_api.PostgresTestcontainersInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(initializers = PostgresTestcontainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentTest {

    @Autowired
    CommentRepository commentRepository;

    @Test
    @Transactional
    @DisplayName("Testing Post entity mapping with database")
    void testComment() {
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