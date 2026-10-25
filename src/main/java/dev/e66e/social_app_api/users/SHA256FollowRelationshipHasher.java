package dev.e66e.social_app_api.users;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
class SHA256FollowRelationshipHasher implements FollowRelationshipHasher {

    @Override
    public String createRelationshipHash(UUID followerId, UUID followTargetId) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((followerId.toString()
                + followTargetId)
                .getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hash);
    }
}
