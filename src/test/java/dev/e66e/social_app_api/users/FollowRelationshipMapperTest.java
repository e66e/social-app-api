package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.FollowRelationship;
import dev.e66e.social_app_api.users.persistence.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FollowRelationshipMapper Unit Tests")
class FollowRelationshipMapperTest {

    private final FollowRelationshipMapper mapper = new FollowRelationshipMapperImpl();

    @Test
    @DisplayName("should map FollowRelationship entity to FollowRelationshipResponse record")
    void followRelationshipToFollowRelationshipResponse_shouldMapAllFields() {
        UUID followerId = UUID.randomUUID();
        UUID followTargetId = UUID.randomUUID();

        User follower = User.builder().id(followerId).username("follower").build();
        User followTarget = User.builder().id(followTargetId).username("target").build();

        FollowRelationship relationship = new FollowRelationship(follower, followTarget);

        FollowRelationshipResponse response = mapper.followRelationshipToFollowRelationshipResponse(relationship);

        assertThat(response).isNotNull();
        assertThat(response.followerId()).isEqualTo(followerId);
        assertThat(response.followTargetId()).isEqualTo(followTargetId);
    }
}
