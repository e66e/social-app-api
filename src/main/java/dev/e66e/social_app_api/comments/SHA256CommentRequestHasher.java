package dev.e66e.social_app_api.comments;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
class SHA256CommentRequestHasher implements CommentRequestHasher {

    @Override
    public String createCommentHash(UUID authorId, CommentRequest commentRequest) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((authorId
                + "\n" + commentRequest.postId()
                + "\n" + commentRequest.content())
                .getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hash);
    }
}
