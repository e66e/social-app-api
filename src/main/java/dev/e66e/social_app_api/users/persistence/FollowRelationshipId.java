package dev.e66e.social_app_api.users.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
class FollowRelationshipId {

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "follow_target_id", nullable = false)
    private UUID followTargetId;
}
