package dev.e66e.social_app_api.comments;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommentMapperTest {

    private final CommentMapper mapper = Mappers.getMapper(CommentMapper.class);

    private static Comment newComment(UUID commentId, UUID postId, UUID parentId,
                                      Instant createdAt, Instant updatedAt) {
        Comment c = new Comment(postId, UUID.randomUUID(), parentId, "hello");
        c.setId(new CommentId(commentId, postId));
        c.setCreatedAt(createdAt);
        c.setUpdatedAt(updatedAt);
        return c;
    }

    @Test
    void mapsAllFieldsToCommentResponse() {
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant updatedAt = createdAt.plusSeconds(60);

        CommentResponse r = mapper.commentToCommentResponse(
                newComment(commentId, postId, parentId, createdAt, updatedAt));

        assertThat(r)
                .isNotNull()
                .returns(commentId, CommentResponse::id)
                .returns(postId, CommentResponse::postId)
                .returns(parentId, CommentResponse::parentCommentId)
                .returns("hello", CommentResponse::content)
                .returns(createdAt, CommentResponse::createdAt)
                .returns(updatedAt, CommentResponse::updatedAt);
    }

    @Test
    void mapsAllFieldsToCommentRecord() {
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        CommentRecord r = mapper.commentToCommentRecord(
                newComment(commentId, postId, null, Instant.now(), null));

        assertThat(r)
                .returns(commentId, CommentRecord::id)
                .returns(postId, CommentRecord::postId)
                .returns(null, CommentRecord::parentCommentId) // No warning
                .returns("hello", CommentRecord::content)
                .returns(null, CommentRecord::updatedAt);
    }
}
