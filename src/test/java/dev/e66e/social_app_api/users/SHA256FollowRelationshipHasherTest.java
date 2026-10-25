package dev.e66e.social_app_api.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SHA256FollowRelationshipHasher Unit Tests")
class SHA256FollowRelationshipHasherTest {

    private final SHA256FollowRelationshipHasher hasher = new SHA256FollowRelationshipHasher();

    @Test
    @DisplayName("produces a 64-character lowercase hexadecimal hash")
    void produces64CharacterHex() {
        UUID followerId = UUID.randomUUID();
        UUID followTargetId = UUID.randomUUID();

        String hash = hasher.createRelationshipHash(followerId, followTargetId);

        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    @DisplayName("produces stable and deterministic hash for identical inputs")
    void producesStableHashForSameInput() {
        UUID followerId = UUID.randomUUID();
        UUID followTargetId = UUID.randomUUID();

        String hash1 = hasher.createRelationshipHash(followerId, followTargetId);
        String hash2 = hasher.createRelationshipHash(followerId, followTargetId);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("produces different hash for different follower")
    void producesDifferentHashForDifferentFollower() {
        UUID follower1 = UUID.randomUUID();
        UUID follower2 = UUID.randomUUID();
        UUID followTarget = UUID.randomUUID();

        String hash1 = hasher.createRelationshipHash(follower1, followTarget);
        String hash2 = hasher.createRelationshipHash(follower2, followTarget);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("produces different hash for different follow target")
    void producesDifferentHashForDifferentFollowTarget() {
        UUID follower = UUID.randomUUID();
        UUID target1 = UUID.randomUUID();
        UUID target2 = UUID.randomUUID();

        String hash1 = hasher.createRelationshipHash(follower, target1);
        String hash2 = hasher.createRelationshipHash(follower, target2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("produces different hash when follower and target IDs are swapped")
    void producesDifferentHashWhenSwapped() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String hashAtoB = hasher.createRelationshipHash(userA, userB);
        String hashBtoA = hasher.createRelationshipHash(userB, userA);

        assertThat(hashAtoB).isNotEqualTo(hashBtoA);
    }
}
