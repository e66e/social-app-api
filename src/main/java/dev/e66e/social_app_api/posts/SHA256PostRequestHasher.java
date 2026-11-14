package dev.e66e.social_app_api.posts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
@Slf4j
class SHA256PostRequestHasher implements PostRequestHasher {

    @Override
    public String createPostHash(UUID authorId, PostRequest postRequest) {
        log.info("Hashing post for user with id={}.", authorId);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found.");
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((authorId
                + "\n" + postRequest.content()
                + "\n" + postRequest.imageUrl())
                .getBytes(StandardCharsets.UTF_8));

        log.info("Hashing completed successfully.");
        return HexFormat.of().formatHex(hash);
    }
}
