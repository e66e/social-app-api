package dev.e66e.social_app_api.comments;

import java.util.UUID;

interface CommentRequestHasher {

    String createCommentHash(UUID authorId, CommentRequest commentRequest);
}
