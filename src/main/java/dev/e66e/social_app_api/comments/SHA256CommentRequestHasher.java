package dev.e66e.social_app_api.comments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
@Slf4j
class SHA256CommentRequestHasher implements CommentRequestHasher {

    @Override
    public String createCommentHash(UUID authorId, CommentRequest commentRequest) {
        log.info("Requested for CommentRequestHasher with arguments: authorId={}, commentRequest={}",
                authorId, commentRequest);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.error("Couldn't find SHA-256 algorithm.");
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((authorId
                + "\n" + commentRequest.postId()
                + "\n" + commentRequest.content()
                + "\n" + commentRequest.parentCommentId())
                .getBytes(StandardCharsets.UTF_8));

        String hashString = HexFormat.of().formatHex(hash);
        log.debug("Created hash: {}", hashString);

        return hashString;
    }
}
