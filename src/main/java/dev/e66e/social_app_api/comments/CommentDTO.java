package dev.e66e.social_app_api.comments;

import java.util.UUID;

public record CommentDTO(
        UUID id,
        UUID postId,
        String content,
        UUID parentCommentId
) {
}
