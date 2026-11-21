package dev.e66e.social_app_api.users;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
@Slf4j
class SHA256FollowRelationshipHasher implements FollowRelationshipHasher {

    @Override
    public String createRelationshipHash(UUID followerId, String followTargetUsername) {
        log.info("Started creating following hash for follower with id={} and follow target with username={}.",
                followerId, followTargetUsername);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }

        byte[] hash = digest.digest((followerId
                + followTargetUsername)
                .getBytes(StandardCharsets.UTF_8));

        log.info("Successfully created following hash for follower with id={} and follow target with username={}.",
                followerId, followTargetUsername);
        return HexFormat.of().formatHex(hash);
    }
}
