package dev.e66e.social_app_api.posts;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
class SHA256PostRequestHasher implements PostRequestHasher {

    @Override
    public String createPostHash(UUID authorId, PostRequest postRequest) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((authorId
                + "\n" + postRequest.content()
                + "\n" + postRequest.imageUrl())
                .getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hash);
    }
}
