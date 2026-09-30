package dev.e66e.social_app_api.users.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
class FollowRelationship {

    @EmbeddedId
    private FollowRelationshipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followTargetId")
    @JoinColumn(name = "follow_target_id", nullable = false)
    private User followTarget;

    public FollowRelationship(User follower, User followTarget) {
        this.id = new FollowRelationshipId(follower.getId(), followTarget.getId());
        this.follower = follower;
        this.followTarget = followTarget;
    }
}
